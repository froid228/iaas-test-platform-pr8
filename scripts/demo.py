#!/usr/bin/env python3
"""Проверка API учебного стенда. Тестируемое ПО этот скрипт не запускает."""
import argparse
import collections
import json
import os
from pathlib import Path
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
API = 'http://localhost:8080'
ISSUER = 'http://localhost:8180/realms/test-platform'


def read_env():
    values = {}
    path = ROOT / '.env'
    if path.exists():
        for line in path.read_text().splitlines():
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                key, value = line.split('=', 1)
                values[key.strip()] = value.strip().strip('\"\'')
    values.update(os.environ)
    return values


def request(url, method='GET', data=None, token=None, form=False, expected=200, request_id=None):
    headers = {}
    if request_id:
        headers["X-Request-Id"] = request_id
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if data is not None:
        headers['Content-Type'] = 'application/x-www-form-urlencoded' if form else 'application/json'
        body = (urllib.parse.urlencode(data) if form else json.dumps(data)).encode()
    else:
        body = None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        response = urllib.request.urlopen(req, timeout=15)
    except urllib.error.HTTPError as ex:
        response = ex
    with response:
        raw = response.read().decode()
        if response.status != expected:
            raise RuntimeError(f'{method} {url}: ожидался {expected}, получен {response.status}: {raw[:500]}')
        return (json.loads(raw) if raw else None), response.headers


def wait_for(action, description):
    deadline = time.monotonic() + 180
    while True:
        try:
            return action()
        except (urllib.error.URLError, TimeoutError, OSError, RuntimeError):
            if time.monotonic() >= deadline:
                raise RuntimeError(f'Не дождались готовности: {description}. Проверьте docker compose logs.') from None
            time.sleep(3)


def main():
    global API
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--balance', action='store_true', help='Только проверить экземпляры project-service')
    args = parser.parse_args()
    settings = read_env()
    API = settings.get('API_BASE_URL', 'http://localhost:' + settings.get('GATEWAY_PORT', '8080')).rstrip('/')
    secret = settings.get('PLATFORM_CLIENT_SECRET')
    if not secret:
        raise RuntimeError('Нет PLATFORM_CLIENT_SECRET. Сначала выполните ./run.sh либо создайте .env.')
    print('Ожидание Keycloak и API (до 3 минут на компонент)...')
    auth, _ = wait_for(lambda: request(
        ISSUER + '/protocol/openid-connect/token', 'POST',
        {'grant_type': 'client_credentials', 'client_id': 'platform-cli', 'client_secret': secret},
        form=True), 'Keycloak')
    token = auth['access_token']
    wait_for(lambda: request(API + '/api/projects', token=token), 'project-service через Gateway')
    if args.balance:
        instances = collections.Counter()
        for _ in range(12):
            _, headers = request(API + '/api/projects', token=token)
            instances[headers.get('X-Service-Instance', 'unknown')] += 1
        for instance, count in instances.items():
            print(f'{instance}: {count} ответов')
        if len(instances) < 2 or 'unknown' in instances:
            raise RuntimeError('Два экземпляра не подтверждены. Проверьте scale=2 и регистрацию в Eureka.')
        return
    wait_for(lambda: request(API + '/api/environments', token=token), 'environment-service')
    wait_for(lambda: request(API + '/api/test-runs', token=token), 'test-run-service')
    request(API + '/api/projects', expected=401)
    print('401 без токена: OK')
    project, _ = request(API + '/api/projects', 'POST', {
        'name': 'Демонстрационное клиент-серверное ПО',
        'softwareVersion': '0.1-demo', 'artifactReference': 'demo://builds/client-server.zip',
    }, token, expected=201)
    environment, _ = request(API + '/api/environments', 'POST', {
        'name': 'Шаблон Linux-стенда', 'operatingSystem': 'Ubuntu 24.04',
        'imageReference': 'demo://images/ubuntu-24.04', 'cpu': 2, 'memoryMb': 2048,
    }, token, expected=201)
    run, _ = request(API + '/api/test-runs', 'POST', {
        'projectId': project['id'], 'environmentId': environment['id'],
        'testSuite': 'manual-api-demonstration',
    }, token, expected=201)
    assert run['status'] == 'QUEUED', run
    running, _ = request(API + '/api/test-runs/' + run['id'] + '/start', 'POST', token=token)
    assert running['status'] == 'RUNNING', running
    result, _ = request(API + '/api/test-runs/' + run['id'] + '/result', 'POST', {
        'status': 'PASSED',
        'summary': 'Учебная запись результата API; дистрибутив ПО не тестировался.',
        'evidenceUri': 'demo://manual-api-example',
    }, token)
    assert result['status'] == 'PASSED', result
    request(API + '/api/test-runs/' + run['id'] + '/start', 'POST', token=token, expected=409)
    request(API + '/api/test-runs', 'POST', {
        'projectId': '00000000-0000-0000-0000-000000000000',
        'environmentId': environment['id'], 'testSuite': 'missing-reference',
    }, token, expected=422)
    target = ROOT / '.runtime/demo-result.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps({'project': project, 'environment': environment, 'run': result},
                                ensure_ascii=False, indent=2) + '\n')
    print('Проект, шаблон стенда, Feign-проверки и переходы запуска: OK')
    print('409 при повторном старте и 422 при отсутствующей ссылке: OK')
    print('Это демонстрация API. ВМ не создавались, тестирование дистрибутива не выполнялось.')
    print('Ответы API:', target)


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, urllib.error.URLError, OSError, AssertionError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
