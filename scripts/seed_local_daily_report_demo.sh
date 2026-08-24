#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
infra_dir="${repo_root}/infra"
env_file="${infra_dir}/.env.local"
seed_file="${repo_root}/docs/sql/local_daily_report_demo_seed.sql"

if [[ ! -f "${env_file}" ]]; then
  echo "Missing ${env_file}. Create it from infra/.env.local.example first." >&2
  exit 1
fi

set -a
source "${env_file}"
set +a

echo "Seeding the Docker-only local PostgreSQL service. Shared databases are not used."

docker compose \
  --env-file "${env_file}" \
  -f "${infra_dir}/docker-compose.yml" \
  -f "${infra_dir}/docker-compose.local.yml" \
  exec -T postgres \
  psql -X -v ON_ERROR_STOP=1 -U "${DB_USERNAME}" -d "${DB_NAME}" \
  < "${seed_file}"

echo "Local demo data is ready."
echo "Login: report.demo@peztz.local / PeztzDemo!2026"
