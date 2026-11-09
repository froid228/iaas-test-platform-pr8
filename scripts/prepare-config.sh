#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .runtime/config-repo
cp config-repo/*.yaml .runtime/config-repo/
if [[ ! -d .runtime/config-repo/.git ]]; then
  git -C .runtime/config-repo init -b main --quiet
fi
git -C .runtime/config-repo add -- '*.yaml'
if ! git -C .runtime/config-repo diff --cached --quiet; then
  git -C .runtime/config-repo -c user.name='PR8 Config' -c user.email='config@example.test' commit --quiet -m 'Update platform configuration'
fi
echo 'Git-репозиторий конфигурации готов: .runtime/config-repo (main)'
