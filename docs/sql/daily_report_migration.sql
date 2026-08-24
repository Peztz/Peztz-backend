-- Add persisted AI daily reports to an existing Peztz PostgreSQL database.

begin;

do $$
begin
    if to_regclass('public."Pets"') is null then
        raise exception 'Required table public."Pets" is missing';
    end if;
end $$;

create table if not exists public.daily_report (
    report_id uuid primary key,
    pet_id uuid not null references public."Pets"(pet_id) on delete cascade,
    report_date date not null,
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
    constraint ck_daily_report_status check (status in ('READY', 'FAILED'))
);

create index if not exists idx_daily_report_pet_date
    on public.daily_report(pet_id, report_date desc);

commit;
