#!/usr/bin/env bash
# MySQL 每日全量备份，保留 7 份。建议宿主机 cron：0 2 * * * /path/deploy/backup.sh
set -euo pipefail
cd "$(dirname "$0")/.."
set -a
[ -f .env ] && . ./.env
set +a
STAMP=$(date +%Y%m%d-%H%M%S)
mkdir -p backups
docker compose exec -T mysql mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" \
  --single-transaction --quick --routines --events --databases "${MYSQL_DATABASE:-xh_boiler}" \
  | gzip > "backups/xh_boiler-${STAMP}.sql.gz"
# 保留最近 7 份
ls -1t backups/xh_boiler-*.sql.gz | tail -n +8 | xargs -r rm -f
echo "backup ok: backups/xh_boiler-${STAMP}.sql.gz"
