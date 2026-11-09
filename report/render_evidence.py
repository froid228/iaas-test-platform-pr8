#!/usr/bin/env python3
"""Рендер протоколов настоящей проверки в HTML и снимки этих страниц браузером.
Не рисует терминал или интерфейс приложения. Данные берутся из report/evidence.
"""
import argparse
import html
import json
from pathlib import Path
import subprocess
import tempfile
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'report/evidence'
VIEWS = EVIDENCE / 'views'
SHOTS = ROOT / 'report/screenshots'
CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
CSS = '''
*{box-sizing:border-box}body{margin:0;padding:38px 44px;background:#eef2f5;color:#162638;font:19px Arial,sans-serif}
header{border-top:7px solid #19736b;padding-top:18px;margin-bottom:24px}h1{font-size:32px;margin:8px 0 12px}h2{font-size:21px;margin:0 0 12px;color:#12675f}
.meta{font-size:16px;color:#506276;line-height:1.6}.grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.card{background:white;border:1px solid #ced8e1;border-radius:10px;padding:20px;margin-bottom:18px;overflow:hidden}
pre{white-space:pre-wrap;word-break:break-word;font:17px/1.4 Menlo,monospace;margin:8px 0 0}.url{font:17px Menlo,monospace;color:#35465a;word-break:break-all}.status{font-weight:bold;color:#12675f}
.small{font-size:15px;color:#526377}footer{margin-top:16px;border-top:1px solid #c5d0da;padding-top:12px;color:#526377;font-size:16px}.trace pre{font-size:16px;line-height:1.5}.compact pre{font-size:16px;line-height:1.28}
'''


def escaped(value): return html.escape(str(value))
def pretty(value): return escaped(json.dumps(value, ensure_ascii=False, indent=2))
def panel(title, text, cls=''):
    return f'<section class="card {cls}"><h2>{escaped(title)}</h2><pre>{escaped(text)}</pre></section>'


def page(name, title, content, captured):
    path = VIEWS / (name + '.html')
    path.write_text(f'''<!doctype html><html lang="ru"><meta charset="utf-8"><title>{escaped(title)}</title><style>{CSS}</style>
<header><div class="meta">IaaS Test Platform · Практическая работа № 8 · Румянцев А. А., ИКБО-11-23</div>
<h1>{escaped(title)}</h1><div class="meta">Протокол проверки работающего REST API и Docker-стенда<br>Дата сбора (UTC): {escaped(captured)}</div></header>
{content}<footer>Источник: сохранённые HTTP-ответы / docker compose / RabbitMQ Management API в report/evidence.<br>OAuth2-токены и секреты исключены. Учебный сценарий API не исполняет тесты дистрибутива ПО.</footer></html>''')
    return path


def record_card(record):
    if 'method' not in record:
        return f'<section class="card"><h2>Выдача токена: HTTP {record["status"]}</h2><pre>{pretty(record)}</pre></section>'
    text = f'<section class="card compact"><h2>{escaped(record["method"])} · HTTP {record["status"]}</h2><div class="url">{escaped(record["url"])}</div>'
    text += f'<p class="small">X-Request-Id: {escaped(record["requestId"])}</p>'
    if 'requestBody' in record: text += '<div class="small">Тело запроса</div><pre>' + pretty(record['requestBody']) + '</pre>'
    text += '<div class="small">Ответ сервера</div><pre>' + pretty(record['body']) + '</pre></section>'
    return text


def build():
    VIEWS.mkdir(parents=True, exist_ok=True)
    SHOTS.mkdir(parents=True, exist_ok=True)
    scenario = json.loads((EVIDENCE/'scenario.json').read_text())
    captured = scenario['capturedAt']
    pages = []
    for i, frame in enumerate(scenario['frames'], 1):
        records = frame['records']
        cards = ''.join(record_card(r) for r in (records[:3] if i == 5 else records))
        if i == 5:
            cards += panel('Дополнительные проверки ошибок', '\n'.join(
                f'{r["method"]} {r["url"]} → HTTP {r["status"]}' for r in records[3:]))
        pages.append((page(f'scenario-{i:02}', frame['title'], '<div class="grid">'+cards+'</div>',captured),f'scenario-{i:02}.png',1200 if i==5 else 1050))
    content = panel('Команда: docker compose ps', (EVIDENCE/'containers.txt').read_text())
    pages.append((page('containers','Работающие контейнеры проекта', content,captured),'containers.png',800))
    trace = (EVIDENCE/'request-trace.txt').read_text()
    pages.append((page('request-trace','Один requestId через четыре сервиса',panel('Gateway → test-run-service → project-service / environment-service',trace,'trace'),captured),'request-trace.png',1050))
    rabbit = (EVIDENCE/'rabbitmq.txt').read_text()
    pages.append((page('rabbitmq','Обмен событием через RabbitMQ',panel('Producer → platform.events → environment.run-created → consumer',rabbit,'trace'),captured),'rabbitmq.png',1050))
    fail = json.loads((EVIDENCE/'failure.json').read_text())
    content = panel('docker compose stop environment-service → docker compose ps --all',fail['containersDuringFailure'])
    content += '<div class="grid">'+record_card(fail['failedRequest'])
    content += panel('Сохранность данных и доступность', f'Запусков до отказа: {fail["runsBefore"]}\nЗапусков после отказа: {fail["runsAfter"]}\nGET /api/projects/{{id}} → HTTP {fail["independentService"]["status"]}\n\nВосстановление:\ndocker compose start environment-service\nPOST /api/test-runs → HTTP {fail["afterRecovery"]["status"]}\nstatus: {fail["afterRecovery"]["body"]["status"]}') + '</div>'
    pages.append((page('failure','Недоступность сервиса и восстановление',content,fail['capturedAt']),'failure.png',1250))
    return pages


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--html-only',action='store_true')
    args=parser.parse_args()
    pages=build()
    if args.html_only: return
    with tempfile.TemporaryDirectory(prefix='pr8-evidence-chrome-') as profile:
        for source, name, height in pages:
            target=SHOTS/name
            result=subprocess.run([CHROME,'--headless','--disable-gpu','--hide-scrollbars','--no-pdf-header-footer',
                '--user-data-dir='+profile,'--window-size=1600,'+str(height),
                '--screenshot='+str(target),source.as_uri()],capture_output=True,text=True,timeout=45)
            if result.returncode or not target.exists(): raise RuntimeError(result.stderr[-1500:])
            print('Screenshot:',target.name,flush=True)
        target=SHOTS/'eureka.png'
        subprocess.run([CHROME,'--headless','--disable-gpu','--hide-scrollbars','--user-data-dir='+profile,
            '--window-size=1600,1100','--screenshot='+str(target),'http://localhost:8761'],check=True,capture_output=True,timeout=45)
        (SHOTS/'03.png').write_bytes(target.read_bytes())


if __name__=='__main__': main()
