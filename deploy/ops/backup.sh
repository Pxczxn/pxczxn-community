#!/usr/bin/env bash
set -euo pipefail

root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
compose=(docker compose --env-file "$root_dir/deploy/.env" -f "$root_dir/deploy/docker-compose.yml")
source "$root_dir/deploy/.env"
backup_root=${BACKUP_DIR:?set BACKUP_DIR in deploy/.env}
retention_days=${BACKUP_RETENTION_DAYS:-14}
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_dir="$backup_root/$timestamp"
mkdir -p "$backup_dir/minio"
umask 077

"${compose[@]}" exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --events --triggers --set-gtid-purged=OFF "$MYSQL_DATABASE"' | gzip -9 > "$backup_dir/mysql.sql.gz"

network="${COMPOSE_PROJECT_NAME:-pxczxn}_default"
docker run --rm --network "$network" -v "$backup_dir/minio:/backup" -e "MC_HOST_source=http://${MINIO_ROOT_USER}:${MINIO_ROOT_PASSWORD}@minio:9000" minio/mc:RELEASE.2025-05-21T01-59-54Z mirror --overwrite source /backup

(cd "$backup_dir" && sha256sum mysql.sql.gz > SHA256SUMS)
{
  printf 'created_at=%s\n' "$timestamp"
  printf 'database=%s\n' "$MYSQL_DATABASE"
  printf 'minio_snapshot=minio/\n'
  printf 'restore_command=deploy/ops/restore.sh %s %s\n' "$timestamp" "$MYSQL_DATABASE"
} > "$backup_dir/manifest.env"

find "$backup_root" -mindepth 1 -maxdepth 1 -type d -mtime +"$retention_days" -exec rm -rf {} +
printf 'backup complete: %s\n' "$backup_dir"
