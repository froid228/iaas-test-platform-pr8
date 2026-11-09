"""Собрать фактические сведения работающего стенда без сохранения токенов и секретов."""
import importlib.util
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'report/evidence'
OUT.mkdir(parents=True, exist_ok=True)
spec = importlib.util.spec_from_file_location('demo', ROOT / 'scripts/demo.py')
demo = importlib.util.module_from_spec(spec)
spec.loader.exec_module(demo)
settings = demo.read_env()
api = 'http://localhost:' + settings.get('GATEWAY_PORT', '8080')


def command(args):
    return subprocess.run(args, cwd=ROOT, capture_output=True, text=True, check=True).stdout.strip()


auth, _ = demo.request(demo.ISSUER + '/protocol/openid-connect/token', 'POST', {
    'grant_type': 'client_credentials', 'client_id': 'platform-cli',
    'client_secret': settings['PLATFORM_CLIENT_SECRET'],
}, form=True)
token = auth['access_token']
demo.request(api + '/api/projects', expected=401)
config, _ = demo.request('http://localhost:8888/project-service/default/main')
health, _ = demo.request('http://localhost:8888/actuator/health')
safe_config = {k: config[k] for k in ['name', 'profiles', 'label', 'version']}
safe_config['propertySources'] = [p['name'] for p in config['propertySources']]
(OUT / 'config.json').write_text(json.dumps(safe_config, ensure_ascii=False, indent=2) + '\n')

containers = []
for line in command(['docker', 'compose', 'ps', '--format', 'json']).splitlines():
    c = json.loads(line)
    containers.append({k:c[k] for k in ['Name', 'Service', 'State', 'Health', 'Ports']})
(OUT / 'containers.json').write_text(json.dumps(containers, ensure_ascii=False, indent=2) + '\n')
table = ['SERVICE                 STATE      HEALTH     PORTS']
for c in sorted(containers, key=lambda c: c['Name']):
    label = c['Service'] + ('-2' if c['Name'].endswith('-2') else '')
    table.append(f"{label:23} {c['State']:10} {c['Health'] or '-':10} {c['Ports'] or 'internal'}")
(OUT / 'containers.txt').write_text('\n'.join(table) + '\n')

databases = []
for service, db in [('project-db','projects'), ('environment-db','environments'), ('test-run-db','test_runs')]:
    result = command(['docker','compose','exec','-T',service,'psql','-U','platform','-d',db,'-Atc',
        'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;'])
    databases.append(f'{service} / {db}: {result}')
(OUT / 'databases.txt').write_text('\n'.join(databases) + '\n')

req = urllib.request.Request('http://localhost:8761/eureka/apps', headers={'Accept':'application/json'})
with urllib.request.urlopen(req, timeout=10) as response:
    registry = json.load(response)
instances = []
for app in registry['applications']['application']:
    for item in app['instance']:
        instances.append({'application':app['name'], 'instanceId':item['instanceId'], 'status':item['status']})
(OUT / 'eureka.json').write_text(json.dumps(instances, ensure_ascii=False, indent=2) + '\n')

tests = []
for p in sorted(ROOT.glob('*/target/surefire-reports/TEST-*.xml')):
    element = ET.parse(p).getroot()
    tests.append({k:element.attrib[k] for k in ['name','tests','failures','errors','skipped']})
(OUT / 'tests.json').write_text(json.dumps(tests, ensure_ascii=False, indent=2) + '\n')
lines = [f"{t['name'].rsplit('.',1)[-1]}: tests={t['tests']}, failures={t['failures']}, errors={t['errors']}" for t in tests]
lines += [f"TOTAL: {sum(int(t['tests']) for t in tests)}; failures={sum(int(t['failures']) for t in tests)}; errors={sum(int(t['errors']) for t in tests)}"]
(OUT / 'tests.txt').write_text('\n'.join(lines) + '\n')

result = json.loads((ROOT / '.runtime/demo-result.json').read_text())
saved, _ = demo.request(api + '/api/test-runs/' + result['run']['id'], token=token)
assert saved['status'] == result['run']['status']
(OUT / 'demo-result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
(OUT / 'http-checks.txt').write_text('\n'.join([
    'POST Keycloak /protocol/openid-connect/token -> 200 (' + auth['token_type'] + '; token omitted)',
    'GET /api/projects without token -> 401',
    'GET /actuator/health (Config Server) -> 200; status=' + health['status'],
    'GET /project-service/default/main -> 200; Git commit=' + config['version'],
    'GET /api/test-runs/' + saved['id'] + ' -> 200; status=' + saved['status'],
]) + '\n')
print('Collected:', OUT)
print('Containers:',len(containers),'; JUnit tests:',sum(int(t['tests']) for t in tests))
