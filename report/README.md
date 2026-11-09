# Отчёт по практической работе № 8

Итоговые файлы: [DOCX](Отчёт_ПиРКСП_ПР8_Румянцев_АА_ИКБО-11-23.docx) и [PDF](Отчёт_ПиРКСП_ПР8_Румянцев_АА_ИКБО-11-23.pdf). Ссылка на GitHub находится в последнем разделе документа и является кликабельной.

В отчёте: схема архитектуры, жизненный цикл запуска, реальная страница Eureka, пять снимков сквозного REST-сценария, отдельные снимки работающих контейнеров, одного requestId в четырёх сервисах, обмена RabbitMQ и реального отказа environment-service с последующим восстановлением. Снимки протоколов сделаны Google Chrome по сохранённым настоящим ответам API и журналам; они не изображают интерфейс приложения. Токены и пароли не сохраняются. BootcampLabs студент прикладывает отдельно по своему указанию.

`template.docx` содержит только стили и параметры страниц. Генерация не зависит от файлов других практических работ. Материалы `evidence/` включают исходные HTTP-ответы, журналы, статистику RabbitMQ и статические HTML-представления для снимков.

## Повторная проверка стенда

```bash
./run.sh
./scripts/demo.py > report/evidence/demo.txt
python3 scripts/verify_submission.py --failure
# --failure временно останавливает environment-service, восстанавливая его в finally.
docker compose up -d --scale project-service=2
# После регистрации двух реплик в Eureka:
./scripts/demo.py --balance > report/evidence/balance.txt
python3 report/collect_evidence.py
python3 report/render_evidence.py
python3 report/draw_diagrams.py
```

`render_evidence.py` использует установленный Google Chrome для macOS; путь задаётся константой `CHROME`. Для другой ОС укажите путь к браузеру. `--html-only` создаёт представления без снимков.

## Пересборка документов

```bash
python3 -m pip install -r report/requirements.txt
python3 report/build_report.py
soffice --headless --convert-to pdf --outdir report report/Отчёт_ПиРКСП_ПР8_Румянцев_АА_ИКБО-11-23.docx
python3 report/update_toc.py
python3 report/build_report.py
soffice --headless --convert-to pdf --outdir report report/Отчёт_ПиРКСП_ПР8_Румянцев_АА_ИКБО-11-23.docx
python3 report/update_toc.py
```

Последний запуск должен вывести `pagination stable: True`; иначе повторите сборку и экспорт. LibreOffice должен быть установлен. Шрифты могут влиять на разбиение страниц.

Локальный Docker Compose не подтверждает развёртывание в облаке/Minikube. PASSED в демонстрации — учебная запись API, а не результат автоматического исполнения тестов дистрибутива. Доставка RabbitMQ после коммита пока не защищена transactional outbox; окно потери уведомления явно описано в отчёте и README.
