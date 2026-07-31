#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo 'usage: native-rollback-release.sh <release-id> [operations-env-file]'
  exit 64
fi

release_id=$1
operations_env=${2:-/etc/pxczxn/ops.env}
[[ $release_id =~ ^[0-9a-f]{7,40}$ ]] || { echo 'invalid release id' >&2; exit 64; }
[[ -r "$operations_env" ]] || { echo "operations env is unreadable: $operations_env" >&2; exit 66; }
set -a
# shellcheck disable=SC1090
source "$operations_env"
set +a

releases_dir=${PXCZXN_RELEASES_DIR:-/app/pxczxn/releases}
backend_service=${PXCZXN_BACKEND_SERVICE:-pxczxn-community.service}
unit_file=${PXCZXN_BACKEND_UNIT_FILE:-/etc/systemd/system/pxczxn-community.service}
release_jar="$releases_dir/$release_id/backend.jar"
[[ -r "$release_jar" ]] || { echo "release is unavailable: $release_id" >&2; exit 66; }
[[ -w "$unit_file" ]] || { echo "unit file is not writable: $unit_file" >&2; exit 66; }

current_jar=$(sed -n 's#^ExecStart=.* -jar \([^ ]*\)$#\1#p' "$unit_file")
[[ -n "$current_jar" && -r "$current_jar" ]] || { echo 'unable to identify current backend jar' >&2; exit 65; }
expected="ROLLBACK ${release_id}"
read -r -p "Roll back application code to ${release_id} without changing MySQL. Type '${expected}' to continue: " confirmation
[[ $confirmation == "$expected" ]] || { echo 'rollback cancelled'; exit 1; }

backup_dir=/opt/pxczxn-backups/$(date -u +%Y%m%dT%H%M%SZ)-rollback-$release_id
install -d -m 0700 "$backup_dir"
cp -p "$unit_file" "$backup_dir/$(basename "$unit_file").before"
escaped_current=$(printf '%s' "$current_jar" | sed 's/[.[\\*^$()+?{|]/\\&/g')
sed -i "s#$escaped_current#$release_jar#" "$unit_file"
systemctl daemon-reload
systemctl restart "$backend_service"
for attempt in $(seq 1 30); do
  if curl -fsS --max-time 3 http://127.0.0.1:8849/api/v1/health >/dev/null; then
    printf 'application rollback complete: %s\n' "$release_id"
    exit 0
  fi
  sleep 2
done
echo 'rollback health check failed; restore the unit file from the recorded backup' >&2
exit 1
