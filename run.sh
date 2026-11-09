#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env ]]; then
  cp .env.example .env
  echo 'Создан .env с локальными учебными значениями. Измените их при необходимости.'
fi
./scripts/prepare-config.sh
if ! docker info >/dev/null 2>&1; then
  echo 'Запустите Docker Desktop и повторите ./run.sh' >&2
  exit 1
fi
docker compose up --build -d
docker compose ps
echo 'Адрес Gateway (хост:порт):'
docker compose port api-gateway 8080
echo 'Keycloak: http://localhost:8180; Eureka: http://localhost:8761'
echo 'Регистрация в Eureka и готовность API могут занять 1–2 минуты. Затем: ./scripts/demo.py'
