#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
infra_dir="${repo_root}/infra"
env_file="${infra_dir}/.env.local"
seed_file="${repo_root}/docs/sql/local_daily_report_ready_mock.sql"

if [[ ! -f "${env_file}" ]]; then
  echo "Missing ${env_file}. Create it from infra/.env.local.example first." >&2
  exit 1
fi

set -a
source "${env_file}"
set +a

echo "Writing a mock READY report to the Docker-only local PostgreSQL service."
echo "This does not call OpenAI and never uses a shared database."

docker compose \
  --env-file "${env_file}" \
  -f "${infra_dir}/docker-compose.yml" \
  -f "${infra_dir}/docker-compose.local.yml" \
  exec -T postgres \
  psql -X -v ON_ERROR_STOP=1 -U "${DB_USERNAME}" -d "${DB_NAME}" \
  < "${seed_file}"

echo "Local READY mock is available through the normal Spring report API."
