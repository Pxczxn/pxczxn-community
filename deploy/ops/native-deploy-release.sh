#!/usr/bin/env bash
set -euo pipefail

# Installs a pre-built, checksum-verified release bundle on the native host.
# The bundle is intentionally application-only: database changes are applied by
# the reviewed migration workflow before a release is approved.
if [[ $# -lt 2 || $# -gt 3 || ${1:-} == '--help' ]]; then
  echo 'usage: native-deploy-release.sh <release-id> <release-bundle.tar.gz> [operations-env-file]'
  exit 64
fi

release_id=$1
bundle=$2
operations_env=${3:-/etc/pxczxn/ops.env}
[[ $(id -u) -eq 0 ]] || { echo 'native deployment must run as root' >&2; exit 77; }
[[ $release_id =~ ^[0-9a-f]{7,40}$ ]] || { echo 'invalid release id' >&2; exit 64; }
[[ -r "$bundle" ]] || { echo "release bundle is unreadable: $bundle" >&2; exit 66; }
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
release_dir="$releases_dir/$release_id"

[[ $releases_dir = /* && $releases_dir != / && ${#releases_dir} -ge 12 ]] || { echo 'PXCZXN_RELEASES_DIR must be a specific absolute path' >&2; exit 64; }
[[ $bundle = /* ]] || { echo 'release bundle must use an absolute path' >&2; exit 64; }
[[ -w "$releases_dir" ]] || { echo "releases directory is not writable: $releases_dir" >&2; exit 66; }
for file in "$backend_unit" "$web_unit" "$nginx_site"; do
  [[ -r "$file" && -w "$file" ]] || { echo "managed file is unavailable: $file" >&2; exit 66; }
done
[[ ! -e "$release_dir" ]] || { echo "release already exists: $release_dir" >&2; exit 73; }

tar_entries=$(tar -tzf "$bundle")
while IFS= read -r entry; do
  [[ $entry != /* && ! $entry =~ (^|/)\.\.(/|$) ]] || { echo 'release bundle contains an unsafe path' >&2; exit 65; }
done <<< "$tar_entries"
for required in backend.jar admin/index.html web/package.json web/package-lock.json web/dist web/node_modules; do
  grep -qx "$required" <<<"$tar_entries" || grep -qx "$required/" <<<"$tar_entries" || { echo "release bundle is missing $required" >&2; exit 65; }
done

stage_dir=$(mktemp -d "$releases_dir/.${release_id}.staging.XXXXXX")
backup_dir=/opt/pxczxn-backups/$(date -u +%Y%m%dT%H%M%SZ)-deploy-$release_id
committed=false
cleanup() {
  rm -rf -- "$stage_dir"
  if [[ $committed != true ]]; then
    rm -rf -- "$release_dir"
  fi
}
trap cleanup EXIT

tar -xzf "$bundle" -C "$stage_dir" --no-same-owner --no-same-permissions
[[ -r "$stage_dir/SHA256SUMS" ]] || { echo 'release bundle is missing SHA256SUMS' >&2; exit 65; }
(cd "$stage_dir" && sha256sum --check --strict SHA256SUMS)
[[ -r "$stage_dir/backend.jar" && -r "$stage_dir/admin/index.html" && -r "$stage_dir/web/package.json" ]] || { echo 'release bundle payload is incomplete' >&2; exit 65; }

install -d -m 0711 "$release_dir"
cp -a "$stage_dir/." "$release_dir/"
chown -R pxczxn:pxczxn "$release_dir"

backup_failure() {
  local exit_code=$?
  trap - ERR
  if [[ -d "$backup_dir" ]]; then
    cp -pf "$backup_dir/$(basename "$backend_unit")" "$backend_unit"
    cp -pf "$backup_dir/$(basename "$web_unit")" "$web_unit"
    cp -pf "$backup_dir/$(basename "$nginx_site")" "$nginx_site"
    systemctl daemon-reload
    systemctl restart "$backend_service" "$web_service" || true
    systemctl reload "$nginx_service" || true
  fi
  exit "$exit_code"
}

install -d -m 0700 "$backup_dir"
cp -p "$backend_unit" "$backup_dir/$(basename "$backend_unit")"
cp -p "$web_unit" "$backup_dir/$(basename "$web_unit")"
cp -p "$nginx_site" "$backup_dir/$(basename "$nginx_site")"
trap backup_failure ERR

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
    committed=true
    rm -f -- "$bundle"
    printf 'native release deployed: %s\n' "$release_id"
    exit 0
  fi
  sleep 2
done
echo 'release health check failed' >&2
false
