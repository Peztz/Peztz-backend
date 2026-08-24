-- Add persisted AI daily reports to an existing Peztz PostgreSQL database.

begin;

do $$
declare
    missing_tables text;
begin
    select string_agg(required_table, ', ' order by required_table)
    into missing_tables
    from unnest(array[
        'public."Pets"',
        'public.access_session',
        'public.pet_logs',
        'public.sensor_reading'
    ]) as required(required_table)
    where to_regclass(required_table) is null;

    if missing_tables is not null then
        raise exception 'Required Peztz tables are missing: %', missing_tables;
    end if;
end $$;

create table if not exists public.daily_report (
    report_id uuid primary key,
    pet_id uuid not null references public."Pets"(pet_id) on delete cascade,
    report_date date not null,
    generation_token uuid,
    status varchar(20) not null,
    total_log_count bigint not null default 0 check (total_log_count >= 0),
    sensor_log_count bigint not null default 0 check (sensor_log_count >= 0),
    average_temperature double precision,
    average_humidity double precision,
    door_open_count bigint not null default 0 check (door_open_count >= 0),
    low_light_count bigint not null default 0 check (low_light_count >= 0),
    content jsonb not null,
    model_name varchar(100),
    error_message varchar(500),
    generated_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_daily_report_pet_date unique (pet_id, report_date),
    constraint ck_daily_report_status check (status in ('GENERATING', 'READY', 'FAILED')),
    constraint ck_daily_report_generation_token check (
        status <> 'GENERATING' or generation_token is not null
    )
);

-- Keep this migration safe to rerun when daily_report was created by an older
-- release that only knew READY and FAILED.
alter table public.daily_report
    add column if not exists generation_token uuid;

alter table public.daily_report
    drop constraint if exists ck_daily_report_status,
    drop constraint if exists ck_daily_report_generation_token;

alter table public.daily_report
    add constraint ck_daily_report_status
        check (status in ('GENERATING', 'READY', 'FAILED')),
    add constraint ck_daily_report_generation_token
        check (status <> 'GENERATING' or generation_token is not null);

create index if not exists idx_daily_report_pet_date
    on public.daily_report(pet_id, report_date desc);

do $$
declare
    invalid_columns text;
begin
    with expected(table_name, column_name, udt_name) as (
        values
            ('sensor_reading', 'session_id', 'int8'),
            ('sensor_reading', 'attribute', 'varchar'),
            ('sensor_reading', 'numeric_value', 'numeric'),
            ('sensor_reading', 'measured_at', 'timestamptz'),
            ('daily_report', 'report_id', 'uuid'),
            ('daily_report', 'pet_id', 'uuid'),
            ('daily_report', 'report_date', 'date'),
            ('daily_report', 'generation_token', 'uuid'),
            ('daily_report', 'status', 'varchar'),
            ('daily_report', 'content', 'jsonb')
    )
    select string_agg(
        format('%I.%I expected %s but found %s',
            expected.table_name,
            expected.column_name,
            expected.udt_name,
            coalesce(actual.udt_name, '<missing>')),
        '; ' order by expected.table_name, expected.column_name)
    into invalid_columns
    from expected
    left join information_schema.columns actual
        on actual.table_schema = 'public'
       and actual.table_name = expected.table_name
       and actual.column_name = expected.column_name
    where actual.column_name is null
       or actual.udt_name <> expected.udt_name;

    if invalid_columns is not null then
        raise exception 'Daily report migration schema validation failed: %', invalid_columns;
    end if;
end $$;

commit;
