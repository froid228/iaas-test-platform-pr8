"""Измерить страницы заголовков по экспортированному PDF для сборки содержания.

Порядок: build_report.py → экспорт PDF → update_toc.py → build_report.py → экспорт PDF.
Повторный запуск update_toc.py сообщает, стабилизировались ли номера страниц.
"""
import json
import re
from pathlib import Path
import docx
import fitz

ROOT = Path(__file__).resolve().parents[1]
STEM = 'Отчёт_ПиРКСП_ПР8_Румянцев_АА_ИКБО-11-23'
document = docx.Document(ROOT / 'report' / (STEM + '.docx'))
titles = [p.text for p in document.paragraphs if p.style.name in ['Heading 1','Heading 2']]
normalize = lambda text: re.sub(r'\s+', '', text)
pages = {}
pdf = fitz.open(ROOT / 'report' / (STEM + '.pdf'))
for index,page in enumerate(pdf):
    # Содержание может занимать несколько страниц; заголовки ищутся по стилям
    # и целым блокам. Строки содержания содержат номер страницы и не совпадают.
    if index == 0:
        continue
    candidates = set()
    for block in page.get_text('blocks'):
        lines = block[4].splitlines()
        for start in range(len(lines)):
            for length in [1,2,3]:
                candidates.add(normalize(''.join(lines[start:start+length])))
    for title in titles:
        if title not in pages and normalize(title) in candidates:
            pages[title] = index + 1
missing = set(titles) - pages.keys()
if missing:
    raise RuntimeError('Не найдены заголовки: ' + str(missing))
target = ROOT / '.runtime/report-pages.json'
previous = json.loads(target.read_text()) if target.exists() else {}
target.parent.mkdir(parents=True,exist_ok=True)
target.write_text(json.dumps(pages, ensure_ascii=False, indent=2) + '\n')
print('Pages:',len(pdf),'; headings:',len(pages),'; pagination stable:',pages==previous)
