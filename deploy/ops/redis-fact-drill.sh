#!/usr/bin/env bash
set -euo pipefail

root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
compose=(docker compose --env-file "$root_dir/deploy/.env" -f "$root_dir/deploy/docker-compose.yml")
source "$root_dir/deploy/.env"
read -r -p "Staging drill only. Type FLUSH_REDIS_STAGING to continue: " confirmation
[[ $confirmation == 'FLUSH_REDIS_STAGING' ]] || { echo 'drill cancelled'; exit 1; }

facts_before=$("${compose[@]}" exec -T mysql sh -c 'mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT CONCAT((SELECT COUNT(*) FROM community_user),CHAR(58),(SELECT COUNT(*) FROM article),CHAR(58),(SELECT COUNT(*) FROM community_moment));"')
"${compose[@]}" exec -T redis sh -c 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" FLUSHDB ASYNC' | grep -qx OK
"${compose[@]}" exec -T redis sh -c 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping' | grep -qx PONG
facts_after=$("${compose[@]}" exec -T mysql sh -c 'mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT CONCAT((SELECT COUNT(*) FROM community_user),CHAR(58),(SELECT COUNT(*) FROM article),CHAR(58),(SELECT COUNT(*) FROM community_moment));"')
[[ $facts_before == "$facts_after" ]] || { echo 'MySQL facts changed during Redis drill'; exit 1; }
printf 'redis drill passed: mysql core facts %s\n' "$facts_after"
