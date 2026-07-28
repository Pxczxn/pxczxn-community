#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then echo 'usage: restore.sh <backup-id> <target-database>'; exit 64; fi
backup_id=$1
target_database=$2
[[ $backup_id =~ ^[0-9]{8}T[0-9]{6}Z$ ]] || { echo 'invalid backup id'; exit 64; }
[[ $target_database =~ ^[A-Za-z0-9_]+$ ]] || { echo 'invalid database name'; exit 64; }

root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
compose=(docker compose --env-file "$root_dir/deploy/.env" -f "$root_dir/deploy/docker-compose.yml")
source "$root_dir/deploy/.env"
backup_dir="${BACKUP_DIR:?set BACKUP_DIR}/$backup_id"
[[ -f "$backup_dir/mysql.sql.gz" && -f "$backup_dir/SHA256SUMS" ]] || { echo 'backup files are missing'; exit 66; }
[[ $target_database == "$MYSQL_DATABASE" ]] || { echo 'target must equal MYSQL_DATABASE from deploy/.env'; exit 64; }
expected="RESTORE ${backup_id} ${target_database}"
read -r -p "This replaces ${target_database}. Type '${expected}' to continue: " confirmation
[[ $confirmation == "$expected" ]] || { echo 'restore cancelled'; exit 1; }

(cd "$backup_dir" && sha256sum --check SHA256SUMS)
"${compose[@]}" stop backend
"${compose[@]}" exec -T mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -e 'DROP DATABASE IF EXISTS \`$target_database\`; CREATE DATABASE \`$target_database\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'"
gzip -dc "$backup_dir/mysql.sql.gz" | "${compose[@]}" exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'

network="${COMPOSE_PROJECT_NAME:-pxczxn}_default"
docker run --rm --network "$network" -v "$backup_dir/minio:/backup:ro" -e "MC_HOST_target=http://${MINIO_ROOT_USER}:${MINIO_ROOT_PASSWORD}@minio:9000" minio/mc:RELEASE.2025-05-21T01-59-54Z mirror --overwrite /backup target
"${compose[@]}" up -d backend
printf 'restore complete: %s -> %s\n' "$backup_id" "$target_database"
