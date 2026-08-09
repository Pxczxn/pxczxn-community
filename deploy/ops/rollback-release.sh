#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! $1 =~ ^[0-9a-f]{7,40}$ ]]; then echo 'usage: rollback-release.sh <approved-git-commit>'; exit 64; fi
commit=$1
root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
git -C "$root_dir" cat-file -e "$commit^{commit}"
read -r -p "Roll back application code to ${commit}? Type ROLLBACK ${commit}: " confirmation
[[ $confirmation == "ROLLBACK $commit" ]] || { echo 'rollback cancelled'; exit 1; }
git -C "$root_dir" checkout --detach "$commit"
docker compose --env-file "$root_dir/deploy/.env" -f "$root_dir/deploy/docker-compose.yml" up -d --build backend web nginx
printf 'application rollback complete; database was not changed\n'
