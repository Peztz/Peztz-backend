#!/usr/bin/env bash
set -euo pipefail

required_variables=(DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD)
for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Missing required environment variable: ${variable_name}" >&2
    exit 1
  fi
done

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required. Install it with: brew install libpq" >&2
  exit 1
fi

export PGHOST="${DB_HOST}"
export PGPORT="${DB_PORT}"
export PGDATABASE="${DB_NAME}"
export PGUSER="${DB_USERNAME}"
export PGPASSWORD="${DB_PASSWORD}"
export PGSSLMODE="${DB_SSLMODE:-require}"
export PGCONNECT_TIMEOUT="${DB_CONNECT_TIMEOUT_SECONDS:-10}"

echo "Checking PostgreSQL ${PGHOST}:${PGPORT}/${PGDATABASE} as ${PGUSER} (sslmode=${PGSSLMODE})"

psql -X -v ON_ERROR_STOP=1 -P pager=off <<'SQL'
begin read only;

select current_database() as database,
       current_user as database_user,
       current_setting('TimeZone') as database_time_zone;

with required_tables(table_name, relation_name) as (
  values
    ('users', 'public.users'),
    ('Pets', 'public."Pets"'),
    ('access_session', 'public.access_session'),
    ('pet_logs', 'public.pet_logs')
)
select table_name,
       case when to_regclass(relation_name) is null then 'MISSING' else 'OK' end as status
from required_tables
order by table_name;

with required_columns(table_name, column_name) as (
  values
    ('Pets', 'pet_id'),
    ('Pets', 'user_id'),
    ('Pets', 'name'),
    ('Pets', 'pet_breed'),
    ('Pets', 'birth_date'),
    ('access_session', 'session_id'),
    ('access_session', 'pet_id'),
    ('access_session', 'created_at'),
    ('access_session', 'ended_at'),
    ('pet_logs', 'log_id'),
    ('pet_logs', 'session_id'),
    ('pet_logs', 'log_type'),
    ('pet_logs', 'data'),
    ('pet_logs', 'created_at')
)
select required.table_name,
       required.column_name,
       case when actual.column_name is null then 'MISSING' else 'OK' end as status
from required_columns required
left join information_schema.columns actual
  on actual.table_schema = 'public'
 and actual.table_name = required.table_name
 and actual.column_name = required.column_name
order by required.table_name, required.column_name;

select case
         when to_regclass('public.daily_report') is null then 'MIGRATION_REQUIRED'
         else 'READY'
       end as daily_report_schema;

select count(*) as logs_last_7_days,
       count(distinct session.pet_id) as pets_with_logs_last_7_days
from public.pet_logs log
join public.access_session session on session.session_id = log.session_id
where log.created_at >= now() - interval '7 days';

rollback;
SQL

echo "Read-only PostgreSQL preflight completed. No rows were changed."
