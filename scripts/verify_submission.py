#!/usr/bin/env python3
"""Сбор настоящих HTTP-ответов, логов, доставки RabbitMQ и проверки отказа.

При --failure кратковременно останавливает environment-service и восстанавливает его
в finally. Не сохраняет OAuth2-токен и секреты. Добавляет учебные записи в БД.
"""
import argparse
import base64
from datetime import datetime, timezone
import json
from pathlib import Path
import subprocess
import time
import urllib.request
import uuid

import demo

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'report/evidence'


def command(*args):
    return subprocess.run(args, cwd=ROOT, text=True, capture_output=True, check=True).stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--failure', action='store_true', help='Также проверить отказ и восстановление сервиса')
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    settings = demo.read_env()
    api = settings.get('API_BASE_URL', 'http://localhost:' + settings.get('GATEWAY_PORT', '8080')).rstrip('/')
    auth, _ = demo.wait_for(lambda: demo.request(demo.ISSUER + '/protocol/openid-connect/token', 'POST', {
        'grant_type': 'client_credentials', 'client_id': 'platform-cli',
        'client_secret': settings['PLATFORM_CLIENT_SECRET'],
    }, form=True), 'Keycloak')
    token = auth['access_token']
    for resource in ['projects', 'environments', 'test-runs']:
        demo.wait_for(lambda: demo.request(api + '/api/' + resource, token=token), resource)
    frames = []
    captured = datetime.now(timezone.utc).isoformat()

    def call(path, method='GET', data=None, expected=200, anonymous=False, request_id=None):
        rid = request_id or ('pr8-' + str(uuid.uuid4()))
        result, headers = demo.request(api + path, method, data, None if anonymous else token,
                                       expected=expected, request_id=rid)
        record = {'method': method, 'url': api + path, 'requestId': rid,
                  'status': expected, 'responseRequestId': headers.get('X-Request-Id'), 'body': result}
        if data is not None: record['requestBody'] = data
        assert record['responseRequestId'] == rid, record
        return result, record

    _, denied = call('/api/projects', expected=401, anonymous=True)
    _, authorized = call('/api/projects')
    frames.append({'title': '1. Авторизация и доступ к API', 'records': [
        {'operation': 'POST Keycloak /protocol/openid-connect/token', 'status': 200,
         'token_type': auth['token_type'], 'expires_in': auth['expires_in'], 'access_token': '[не сохраняется]'},
        denied, {**authorized, 'body': {'existingProjects': len(authorized['body'])}}]})
    project, rec = call('/api/projects', 'POST', {
        'name': 'Клиент-серверное ПО — проверка ПР8', 'softwareVersion': '0.2-demo',
        'artifactReference': 'demo://builds/client-server.zip'}, expected=201)
    frames.append({'title': '2. Регистрация версии ПО', 'records': [rec]})
    environment, rec = call('/api/environments', 'POST', {
        'name': 'Linux-стенд ПР8', 'operatingSystem': 'Ubuntu 24.04',
        'imageReference': 'demo://images/ubuntu-24.04', 'cpu': 2, 'memoryMb': 2048}, expected=201)
    frames.append({'title': '3. Регистрация шаблона стенда', 'records': [rec]})
    trace_id = 'pr8-create-' + str(uuid.uuid4())
    run_input = {'projectId': project['id'], 'environmentId': environment['id'], 'testSuite': 'manual-api-demonstration'}
    run, rec = call('/api/test-runs', 'POST', run_input, expected=201, request_id=trace_id)
    assert run['status'] == 'QUEUED'
    frames.append({'title': '4. Создание запуска и проверка ссылок', 'records': [rec]})
    running, start = call('/api/test-runs/' + run['id'] + '/start', 'POST')
    assert running['status'] == 'RUNNING'
    result, complete = call('/api/test-runs/' + run['id'] + '/result', 'POST', {
        'status': 'PASSED', 'summary': 'Учебная запись результата API; дистрибутив ПО не тестировался.',
        'evidenceUri': 'demo://manual-api-example'})
    saved, saved_rec = call('/api/test-runs/' + run['id'])
    assert saved['status'] == 'PASSED'
    _, conflict = call('/api/test-runs/' + run['id'] + '/start', 'POST', expected=409)
    _, missing = call('/api/test-runs', 'POST', {**run_input, 'projectId': str(uuid.UUID(int=0))}, expected=422)
    # Для компактного снимка последнего шага отображаем значимые поля реальных ответов.
    compact = []
    for r in [start, complete, saved_rec, conflict, missing]:
        r = dict(r)
        if r['status'] == 200:
            r['body'] = {k:r['body'][k] for k in ['id','status','revision','startedAt','finishedAt']}
        compact.append(r)
    frames.append({'title': '5. Выполнение и сохранение результата', 'records': compact})
    (OUT / 'scenario.json').write_text(json.dumps({'capturedAt': captured, 'frames': frames}, ensure_ascii=False, indent=2) + '\n')
    result_data = {'project': project, 'environment': environment, 'run': result}
    (ROOT / '.runtime/demo-result.json').write_text(json.dumps(result_data, ensure_ascii=False, indent=2) + '\n')
    (OUT / 'demo-result.json').write_text(json.dumps(result_data, ensure_ascii=False, indent=2) + '\n')
    print('Основной сценарий: 401 → 201 project → 201 environment → 201 QUEUED → 200 RUNNING → 200 PASSED; 409/422 OK', flush=True)
    deadline = time.monotonic() + 60
    while True:
        log = command('docker','compose','logs','--no-color','--no-log-prefix','--since','10m',
                      'api-gateway','test-run-service','project-service','environment-service')
        trace_lines = [line for line in log.splitlines() if 'requestId=' + trace_id in line]
        consumed = [line for line in trace_lines if 'event=run.created' in line and 'consumed queue=' in line]
        if consumed: break
        if time.monotonic() > deadline: raise RuntimeError('Не подтверждено потребление события RabbitMQ')
        time.sleep(2)
    for service in ['api-gateway','test-run-service','project-service','environment-service']:
        assert any('service=' + service in line for line in trace_lines), service
    (OUT / 'request-trace.txt').write_text('X-Request-Id: ' + trace_id + '\n\n' + '\n'.join(trace_lines) + '\n')
    event_lines = [line for line in trace_lines if 'event=run.created' in line]
    assert any('published' in line for line in event_lines)
    event_id = consumed[0].split('eventId=')[1].split()[0]
    assert all('eventId=' + event_id in line for line in event_lines)
    credential = base64.b64encode(('platform:' + settings['RABBITMQ_PASSWORD']).encode()).decode()
    broker_req = urllib.request.Request('http://localhost:15672/api/queues/%2F/environment.run-created',
                                        headers={'Authorization':'Basic ' + credential})
    with urllib.request.urlopen(broker_req, timeout=10) as response: queue = json.load(response)
    safe_queue = {k:queue.get(k) for k in ['name','vhost','durable','consumers','messages_ready','messages_unacknowledged','message_stats']}
    (OUT / 'rabbitmq.json').write_text(json.dumps(safe_queue, ensure_ascii=False, indent=2) + '\n')
    (OUT / 'rabbitmq.txt').write_text('Exchange: platform.events\nRouting key: run.created\nQueue: environment.run-created\nEvent ID: ' + event_id + '\n\n' + '\n'.join(event_lines) + '\n\n' + json.dumps(safe_queue, ensure_ascii=False, indent=2) + '\n')
    print('RequestId подтверждён в четырёх сервисах; RabbitMQ: producer → broker → consumer, совпадает eventId', flush=True)
    if args.failure:
        before, _ = call('/api/test-runs')
        failure = {'capturedAt': datetime.now(timezone.utc).isoformat(), 'stoppedService': 'environment-service'}
        try:
            print('Временно останавливаю environment-service...', flush=True)
            failure['stopOutput'] = command('docker','compose','stop','environment-service')
            failure['containersDuringFailure'] = command('docker','compose','ps','--all','--format','table {{.Service}}\t{{.State}}\t{{.Status}}')
            _, unavailable = call('/api/test-runs', 'POST', run_input, expected=503)
            after, _ = call('/api/test-runs')
            assert {r['id'] for r in before} == {r['id'] for r in after}, 'При отказе не должна создаваться запись'
            _, independent = call('/api/projects/' + project['id'])
            failure.update({'failedRequest': unavailable, 'runsBefore':len(before), 'runsAfter':len(after),
                            'independentService': independent})
        finally:
            command('docker','compose','start','environment-service')
            demo.wait_for(lambda: demo.request(api + '/api/environments/' + environment['id'], token=token), 'восстановление environment-service')
            print('environment-service снова доступен', flush=True)
        restored, rec = call('/api/test-runs', 'POST', run_input, expected=201)
        failure['afterRecovery'] = rec
        (OUT / 'failure.json').write_text(json.dumps(failure, ensure_ascii=False, indent=2) + '\n')
        (OUT / 'failure.txt').write_text(json.dumps(failure, ensure_ascii=False, indent=2) + '\n')
        print('Отказ: HTTP 503, лишняя запись не создана, project-service доступен; после восстановления HTTP 201', flush=True)
    print('Материалы сохранены: report/evidence (без токенов и паролей)', flush=True)


if __name__ == '__main__':
    main()
