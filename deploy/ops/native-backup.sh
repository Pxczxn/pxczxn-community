#!/usr/bin/env bash
set -euo pipefail

# Creates a consistent native-host recovery point for MySQL and the private
# MinIO bucket. Credentials must only be supplied through host-only files.
if [[ $# -gt 1 || ${1:-} == '--help' ]]; then
  echo 'usage: native-backup.sh [operations-env-file]'
  exit 64
fi

operations_env=${1:-/etc/pxczxn/ops.env}
[[ -r "$operations_env" ]] || { echo "operations env is unreadable: $operations_env" >&2; exit 66; }
set -a
# shellcheck disable=SC1090
source "$operations_env"
set +a

backup_root=${PXCZXN_BACKUP_DIR:?set PXCZXN_BACKUP_DIR}
database=${PXCZXN_DATABASE:?set PXCZXN_DATABASE}
minio_env=${PXCZXN_MINIO_ENV:?set PXCZXN_MINIO_ENV}
minio_endpoint=${PXCZXN_MINIO_ENDPOINT:-http://127.0.0.1:9000}
minio_bucket=${PXCZXN_MINIO_BUCKET:?set PXCZXN_MINIO_BUCKET}
retention_days=${BACKUP_RETENTION_DAYS:-14}

[[ $database =~ ^[A-Za-z0-9_]+$ ]] || { echo 'invalid PXCZXN_DATABASE' >&2; exit 64; }
[[ $minio_bucket =~ ^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$ ]] || { echo 'invalid PXCZXN_MINIO_BUCKET' >&2; exit 64; }
[[ $retention_days =~ ^[0-9]+$ ]] || { echo 'invalid BACKUP_RETENTION_DAYS' >&2; exit 64; }
[[ $backup_root = /* && $backup_root != / && ${#backup_root} -ge 12 ]] || { echo 'PXCZXN_BACKUP_DIR must be a specific absolute path' >&2; exit 64; }
[[ -r "$minio_env" ]] || { echo "MinIO env is unreadable: $minio_env" >&2; exit 66; }

mysql_defaults=()
if [[ -n ${PXCZXN_MYSQL_DEFAULTS_FILE:-} ]]; then
  [[ -r "$PXCZXN_MYSQL_DEFAULTS_FILE" ]] || { echo 'MySQL defaults file is unreadable' >&2; exit 66; }
  mysql_defaults=("--defaults-extra-file=$PXCZXN_MYSQL_DEFAULTS_FILE")
fi

umask 077
install -d -m 0700 "$backup_root"
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_dir="$backup_root/$timestamp"
[[ ! -e "$backup_dir" ]] || { echo "backup already exists: $backup_dir" >&2; exit 73; }
install -d -m 0700 "$backup_dir/minio"
mc_config=$(mktemp -d /tmp/pxczxn-backup-mc.XXXXXX)
trap 'rm -rf "$mc_config"' EXIT

mysqldump "${mysql_defaults[@]}" \
  --single-transaction --routines --events --triggers --set-gtid-purged=OFF \
  "$database" | gzip -9 > "$backup_dir/mysql.sql.gz"

set -a
# shellcheck disable=SC1090
source "$minio_env"
set +a
MC_CONFIG_DIR="$mc_config" mc alias set local "$minio_endpoint" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null
MC_CONFIG_DIR="$mc_config" mc mirror --overwrite "local/$minio_bucket" "$backup_dir/minio" >/dev/null

(
  cd "$backup_dir"
  find mysql.sql.gz minio -type f -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS
)
{
  printf 'created_at=%s\n' "$timestamp"
  printf 'database=%s\n' "$database"
  printf 'minio_bucket=%s\n' "$minio_bucket"
  printf 'minio_snapshot=minio/\n'
  printf 'restore_command=deploy/ops/native-restore.sh %s %s\n' "$timestamp" "$database"
} > "$backup_dir/manifest.env"

find "$backup_root" -mindepth 1 -maxdepth 1 -type d \
  -regextype posix-extended -regex '.*/[0-9]{8}T[0-9]{6}Z' \
  -mtime +"$retention_days" -print0 | xargs -0 -r rm -rf --

printf 'backup complete: %s\n' "$backup_dir"
