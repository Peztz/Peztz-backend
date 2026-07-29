-- Peztz production-domain migration for existing PostgreSQL tables.
--
-- Apply this file before deploying the Spring Boot domain API build.
-- It reuses existing production tables and only adds what the Spring API
-- needs for authentication tokens and generated numeric IDs.

alter table public.users
    alter column password type varchar(255);

create table if not exists public.auth_token (
    id uuid primary key,
    token varchar(255) not null unique,
    user_id uuid not null references public.users(user_id) on delete cascade,
    created_at timestamp not null,
    expires_at timestamp not null
);

create index if not exists idx_auth_token_user_id on public.auth_token(user_id);
create index if not exists idx_auth_token_expires_at on public.auth_token(expires_at);

create sequence if not exists public.access_session_session_id_seq;

alter table public.access_session
    alter column session_id set default nextval('public.access_session_session_id_seq');

alter table public.access_session
    add column if not exists ended_at timestamp with time zone;

select setval(
    'public.access_session_session_id_seq',
    coalesce((select max(session_id) from public.access_session), 0) + 1,
    false
);

create sequence if not exists public.pet_logs_log_id_seq;

alter table public.pet_logs
    alter column log_id set default nextval('public.pet_logs_log_id_seq');

select setval(
    'public.pet_logs_log_id_seq',
    coalesce((select max(log_id) from public.pet_logs), 0) + 1,
    false
);

alter table public.cage
    add column if not exists hospital_id uuid references public.hospitals(hospital_id),
    add column if not exists name varchar(100),
    add column if not exists cage_number varchar(50),
    add column if not exists created_at timestamp;

update public.cage
set created_at = now()
where created_at is null;

alter table public.cage
    alter column created_at set default now(),
    alter column created_at set not null;

create index if not exists idx_cage_hospital_id on public.cage(hospital_id);

-- Camera metadata. A Cage has exactly one camera. The Cage owns the
-- user, current pet, and Raspberry Pi relationships, so they are not
-- duplicated here. RTSP URLs and credentials are not stored here.
create table if not exists public.camera (
    camera_id uuid primary key,
    cage_id uuid not null unique references public.cage(cage_id) on delete restrict,
    name varchar(100) not null,
    status varchar(50) not null,
    stream_status varchar(50) not null,
    rtsp_config_key varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

-- Abnormal behavior event metadata. MP4 and thumbnail binaries belong in
-- object storage; only their URLs and event metadata are stored in PostgreSQL.
create table if not exists public.pet_event (
    event_id uuid primary key,
    external_event_id varchar(100) not null unique,
    pet_id uuid not null,
    camera_id uuid not null references public.camera(camera_id) on delete restrict,
    event_type varchar(50) not null,
    confidence double precision not null check (confidence >= 0 and confidence <= 1),
    occurred_at timestamp with time zone not null,
    video_url text,
    thumbnail_url text,
    metadata_json jsonb not null default '{}'::jsonb,
    created_at timestamp with time zone not null
);

do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'fk_pet_event_pet'
    ) then
        if to_regclass('public.pets') is not null then
            execute 'alter table public.pet_event add constraint fk_pet_event_pet '
                    'foreign key (pet_id) references public.pets(pet_id) on delete restrict';
        elsif to_regclass('public."Pets"') is not null then
            execute 'alter table public.pet_event add constraint fk_pet_event_pet '
                    'foreign key (pet_id) references public."Pets"(pet_id) on delete restrict';
        else
            raise exception 'Neither public.pets nor public."Pets" exists';
        end if;
    end if;
end $$;

create index if not exists idx_pet_event_pet_occurred_at
    on public.pet_event(pet_id, occurred_at desc);
create index if not exists idx_pet_event_camera_occurred_at
    on public.pet_event(camera_id, occurred_at desc);
