#!/usr/bin/env bash
set -euo pipefail

# Rolls application code back as one native release. Database schema and data
# are deliberately outside this operation and are never rolled back here.
if [[ $# -lt 1 || $# -gt 2 || ${1:-} == '--help' ]]; then
  echo 'usage: native-rollback-release.sh <release-id> [operations-env-file]'
  exit 64
fi

release_id=$1
operations_env=${2:-/etc/pxczxn/ops.env}
[[ $(id -u) -eq 0 ]] || { echo 'native rollback must run as root' >&2; exit 77; }
[[ $release_id =~ ^[0-9a-f]{7,40}$ ]] || { echo 'invalid release id' >&2; exit 64; }
[[ -r "$operations_env" ]] || { echo "operations env is unreadable: $operations_env" >&2; exit 66; }
set -a
# shellcheck disable=SC1090
source "$operations_env"
set +a

releases_dir=${PXCZXN_RELEASES_DIR:-/app/pxczxn/releases}
backend_service=${PXCZXN_BACKEND_SERVICE:-pxczxn-community.service}
backend_unit=${PXCZXN_BACKEND_UNIT_FILE:-/etc/systemd/system/pxczxn-community.service}
web_service=${PXCZXN_WEB_SERVICE:-pxczxn-web.service}
web_unit=${PXCZXN_WEB_UNIT_FILE:-/etc/systemd/system/pxczxn-web.service}
nginx_service=${PXCZXN_NGINX_SERVICE:-nginx.service}
nginx_site=${PXCZXN_NGINX_SITE_FILE:-/etc/nginx/sites-enabled/pxczxn}
backend_health_url=${PXCZXN_BACKEND_HEALTH_URL:-http://127.0.0.1:8849/api/v1/health}
web_health_url=${PXCZXN_WEB_HEALTH_URL:-http://127.0.0.1:8847/}
operation_backup_root=${PXCZXN_OPERATION_BACKUP_DIR:-/opt/pxczxn-backups}
release_dir="$releases_dir/$release_id"

[[ $releases_dir = /* && $releases_dir != / && ${#releases_dir} -ge 12 ]] || { echo 'PXCZXN_RELEASES_DIR must be a specific absolute path' >&2; exit 64; }
[[ $operation_backup_root = /* && $operation_backup_root != / && ${#operation_backup_root} -ge 12 ]] || { echo 'PXCZXN_OPERATION_BACKUP_DIR must be a specific absolute path' >&2; exit 64; }
for file in "$release_dir/backend.jar" "$release_dir/admin/index.html" "$release_dir/web/package.json"; do
  [[ -r "$file" ]] || { echo "complete release is unavailable: $release_id" >&2; exit 66; }
done
for directory in "$release_dir/web/dist" "$release_dir/web/node_modules"; do
  [[ -d "$directory" ]] || { echo "complete release is unavailable: $release_id" >&2; exit 66; }
done
for file in "$backend_unit" "$web_unit" "$nginx_site"; do
  [[ -r "$file" && -w "$file" ]] || { echo "managed file is unavailable: $file" >&2; exit 66; }
done

expected="ROLLBACK ${release_id}"
read -r -p "Roll back backend, web, and admin assets to ${release_id} without changing MySQL. Type '${expected}' to continue: " confirmation
[[ $confirmation == "$expected" ]] || { echo 'rollback cancelled'; exit 1; }

backup_dir="$operation_backup_root/$(date -u +%Y%m%dT%H%M%SZ)-rollback-$release_id"
install -d -m 0700 "$backup_dir"
cp -p "$backend_unit" "$backup_dir/$(basename "$backend_unit").before"
cp -p "$web_unit" "$backup_dir/$(basename "$web_unit").before"
cp -p "$nginx_site" "$backup_dir/$(basename "$nginx_site").before"

restore_previous_configuration() {
  local exit_code=$?
  trap - ERR
  cp -pf "$backup_dir/$(basename "$backend_unit").before" "$backend_unit"
  cp -pf "$backup_dir/$(basename "$web_unit").before" "$web_unit"
  cp -pf "$backup_dir/$(basename "$nginx_site").before" "$nginx_site"
  systemctl daemon-reload
  systemctl restart "$backend_service" "$web_service" || true
  systemctl reload "$nginx_service" || true
  exit "$exit_code"
}
trap restore_previous_configuration ERR

escaped_release_dir=$(printf '%s' "$release_dir" | sed 's/[&\\]/\\&/g')
sed -E "s#^(ExecStart=.* -jar ).*\$#\\1${escaped_release_dir}/backend.jar#" "$backend_unit" > "$backup_dir/backend.unit.next"
grep -qx "ExecStart=/usr/bin/java -Dserver.address=127.0.0.1 -jar $release_dir/backend.jar" "$backup_dir/backend.unit.next" || { echo 'unexpected backend unit format' >&2; exit 65; }
sed -E "s#^(WorkingDirectory=).*/web\$#\\1${escaped_release_dir}/web#" "$web_unit" > "$backup_dir/web.unit.next"
grep -qx "WorkingDirectory=$release_dir/web" "$backup_dir/web.unit.next" || { echo 'unexpected web unit format' >&2; exit 65; }
admin_alias_count=$(grep -Ec '/app/pxczxn/releases/[0-9a-f]{7,40}/admin/' "$nginx_site" || true)
[[ $admin_alias_count -eq 2 ]] || { echo 'unexpected Nginx admin release aliases' >&2; exit 65; }
sed -E "s#/app/pxczxn/releases/[0-9a-f]{7,40}/admin/#${escaped_release_dir}/admin/#g" "$nginx_site" > "$backup_dir/nginx.site.next"
grep -qF "$release_dir/admin/" "$backup_dir/nginx.site.next" || { echo 'Nginx release aliases were not updated' >&2; exit 65; }

install -m 0644 "$backup_dir/backend.unit.next" "$backend_unit"
install -m 0644 "$backup_dir/web.unit.next" "$web_unit"
install -m 0644 "$backup_dir/nginx.site.next" "$nginx_site"
nginx -t
systemctl daemon-reload
systemctl restart "$backend_service" "$web_service"
systemctl reload "$nginx_service"

for attempt in $(seq 1 30); do
  if curl -fsS --max-time 3 "$backend_health_url" >/dev/null && curl -fsS --max-time 3 "$web_health_url" >/dev/null; then
    printf 'native application rollback complete: %s\n' "$release_id"
    exit 0
  fi
  sleep 2
done
echo 'rollback health check failed; original configuration was restored' >&2
false
