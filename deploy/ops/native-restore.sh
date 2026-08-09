#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo 'usage: native-restore.sh <backup-id> <target-database> [operations-env-file]'
  exit 64
fi

backup_id=$1
target_database=$2
operations_env=${3:-/etc/pxczxn/ops.env}
[[ $backup_id =~ ^[0-9]{8}T[0-9]{6}Z$ ]] || { echo 'invalid backup id' >&2; exit 64; }
[[ $target_database =~ ^[A-Za-z0-9_]+$ ]] || { echo 'invalid target database' >&2; exit 64; }
[[ -r "$operations_env" ]] || { echo "operations env is unreadable: $operations_env" >&2; exit 66; }
set -a
# shellcheck disable=SC1090
source "$operations_env"
set +a

backup_root=${PXCZXN_BACKUP_DIR:?set PXCZXN_BACKUP_DIR}
database=${PXCZXN_DATABASE:?set PXCZXN_DATABASE}
backend_service=${PXCZXN_BACKEND_SERVICE:-pxczxn-community.service}
minio_env=${PXCZXN_MINIO_ENV:?set PXCZXN_MINIO_ENV}
minio_endpoint=${PXCZXN_MINIO_ENDPOINT:-http://127.0.0.1:9000}
minio_bucket=${PXCZXN_MINIO_BUCKET:?set PXCZXN_MINIO_BUCKET}
backup_dir="$backup_root/$backup_id"

[[ $target_database == "$database" ]] || { echo 'target database must equal PXCZXN_DATABASE' >&2; exit 64; }
[[ -f "$backup_dir/mysql.sql.gz" && -f "$backup_dir/SHA256SUMS" && -d "$backup_dir/minio" ]] || { echo 'backup files are incomplete' >&2; exit 66; }
[[ -r "$minio_env" ]] || { echo "MinIO env is unreadable: $minio_env" >&2; exit 66; }

mysql_defaults=()
if [[ -n ${PXCZXN_MYSQL_DEFAULTS_FILE:-} ]]; then
  [[ -r "$PXCZXN_MYSQL_DEFAULTS_FILE" ]] || { echo 'MySQL defaults file is unreadable' >&2; exit 66; }
  mysql_defaults=("--defaults-extra-file=$PXCZXN_MYSQL_DEFAULTS_FILE")
fi

expected="RESTORE ${backup_id} ${target_database}"
read -r -p "This replaces ${target_database} and its object bucket. Type '${expected}' to continue: " confirmation
[[ $confirmation == "$expected" ]] || { echo 'restore cancelled'; exit 1; }

(
  cd "$backup_dir"
  sha256sum --check SHA256SUMS
)

mc_config=$(mktemp -d /tmp/pxczxn-restore-mc.XXXXXX)
backend_started=0
restart_backend() {
  if [[ $backend_started == 0 ]]; then
    systemctl start "$backend_service" || true
    backend_started=1
  fi
}
cleanup() {
  restart_backend
  rm -rf "$mc_config"
}
trap cleanup EXIT

systemctl stop "$backend_service"
mysql "${mysql_defaults[@]}" -e "DROP DATABASE IF EXISTS \`$target_database\`; CREATE DATABASE \`$target_database\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
gzip -dc "$backup_dir/mysql.sql.gz" | mysql "${mysql_defaults[@]}" "$target_database"

set -a
# shellcheck disable=SC1090
source "$minio_env"
set +a
MC_CONFIG_DIR="$mc_config" mc alias set local "$minio_endpoint" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null
MC_CONFIG_DIR="$mc_config" mc mirror --remove --overwrite "$backup_dir/minio" "local/$minio_bucket" >/dev/null

systemctl start "$backend_service"
backend_started=1
printf 'restore complete: %s -> %s\n' "$backup_id" "$target_database"
