#!/usr/bin/env bash
# 恢复 MySQL 备份：./deploy/restore.sh backups/xh_boiler-20261031-020000.sql.gz
set -euo pipefail
cd "$(dirname "$0")/.."
if [ $# -lt 1 ]; then echo "usage: $0 <backup.sql.gz>"; exit 1; fi
set -a
[ -f .env ] && . ./.env
set +a
gunzip -c "$1" | docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}"
echo "restore ok: $1"
