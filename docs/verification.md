# Проверка проекта

Дата: 09.11.2026. Среда: macOS arm64, OpenJDK 23.0.1, Maven 3.9.11; целевой байткод Java 21.

## Сборка

Полная команда `./mvnw -B verify` с JDK 23 и локальным Maven-кешем `.runtime/m2` завершилась BUILD SUCCESS для всех шести приложений. Итоговый лог: `.runtime/updated-tests.log`.

| Набор | Тестов | Ошибок / неудач |
| --- | ---: | ---: |
| EnvironmentServiceTest | 2 | 0 / 0 |
| ProjectApiTest | 5 | 0 / 0 |
| ProjectServiceTest | 2 | 0 / 0 |
| RequestIdFeignConfigTest | 2 | 0 / 0 |
| RunEventPublisherTest | 2 | 0 / 0 |
| RunServiceTest | 3 | 0 / 0 |
| TestRunTest | 5 | 0 / 0 |
| **Всего** | **21** | **0 / 0** |

Новые проверки подтверждают передачу requestId в Feign, отсутствие отправки RabbitMQ до коммита и при rollback. Существующие проверки покрывают security, валидацию, внешние ссылки, сохранение и переходы состояний.

## Настоящий Docker-стенд

`./run.sh` собирает и запускает 11 контейнеров: шесть Java-приложений, Keycloak, RabbitMQ и три PostgreSQL. При двух репликах project-service — 12. Порт Gateway в текущем `.env`: 8088. Тома БД сохранены.

- `scripts/demo.py`: получен OAuth2-токен, проверен 401 без токена, созданы проект, шаблон и запуск; переходы QUEUED → RUNNING → PASSED; 409 при повторном старте; 422 при отсутствующей ссылке.
- `scripts/verify_submission.py --failure`: пять этапов API сохранены в `report/evidence/scenario.json`; один requestId подтверждён в Gateway, test-run-service, project-service, environment-service.
- RabbitMQ: публикация `run.created` и получение environment-service подтверждены одинаковым eventId и requestId. Очередь и статистика дополнительно проверены через Management API. Файлы: `rabbitmq.txt`, `rabbitmq.json`.
- Натурный отказ: environment-service остановлен через Docker Compose. POST создания вернул 503; список UUID запусков до и после совпал. Чтение проекта вернуло 200. Сервис восстановлен в finally; создание снова вернуло 201. Файл: `failure.json`.
- Две реплики project-service: 12 запросов распределены 6/6. Снимок Eureka и состояние контейнеров обновлены.
- В каждой отдельной БД успешно применена миграция Flyway версии 1.

Снимки протоколов настоящих HTTP-ответов и журналов сделаны headless Chrome. Они прямо обозначены как протокол проверки REST API. Токены и пароли исключены из сохраняемых материалов.

## Границы результата

PASSED — учебная запись API; настоящий дистрибутив не исполняется, ВМ не создаётся. Docker Compose не заменяет отдельное требование развёртывания в облаке/Minikube. Снимок BootcampLabs по указанию студента прикладывается отдельно. RabbitMQ работает без transactional outbox, поэтому гарантии доставки после сбоя публикации не заявляются.
