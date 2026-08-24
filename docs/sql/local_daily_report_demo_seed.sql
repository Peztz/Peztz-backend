-- Local-only deterministic seed for testing the full AI daily-report flow.
-- Run through scripts/seed_local_daily_report_demo.sh, never against a shared DB.

begin;

insert into public.users (user_id, email, password, name, role)
values (
    '11111111-1111-4111-8111-111111111111',
    'report.demo@peztz.local',
    '$2b$12$UYeLXlDERo4seITJDLp6M.83B9RNxZpI/kHETD7./tWaLCFJTmHy6',
    '리포트 테스트 보호자',
    'OWNER'
)
on conflict (user_id) do update set
    email = excluded.email,
    password = excluded.password,
    name = excluded.name,
    role = excluded.role;

insert into public."Pets" (pet_id, user_id, name, pet_breed, birth_date, medical_note)
values (
    '22222222-2222-4222-8222-222222222222',
    '11111111-1111-4111-8111-111111111111',
    '초코',
    '푸들',
    date '2022-03-01',
    '로컬 AI 일일 리포트 화면 검증용 합성 데이터'
)
on conflict (pet_id) do update set
    user_id = excluded.user_id,
    name = excluded.name,
    pet_breed = excluded.pet_breed,
    birth_date = excluded.birth_date,
    medical_note = excluded.medical_note;

insert into public.cage (
    cage_id, name, cage_number, user_id, current_pet_id, access_code, status, created_at
)
values (
    '33333333-3333-4333-8333-333333333333',
    '로컬 리포트 테스트 케이지',
    'DEMO-01',
    '11111111-1111-4111-8111-111111111111',
    '22222222-2222-4222-8222-222222222222',
    'LOCAL-REPORT-DEMO',
    'OCCUPIED',
    now() at time zone 'Asia/Seoul'
)
on conflict (cage_id) do update set
    name = excluded.name,
    cage_number = excluded.cage_number,
    user_id = excluded.user_id,
    current_pet_id = excluded.current_pet_id,
    access_code = excluded.access_code,
    status = excluded.status;

insert into public.access_session (
    session_id, user_id, pet_id, cage_id, access_code, status, created_at, ended_at
)
values (
    -900001,
    '11111111-1111-4111-8111-111111111111',
    '22222222-2222-4222-8222-222222222222',
    '33333333-3333-4333-8333-333333333333',
    'LOCAL-REPORT-DEMO',
    'ACTIVE',
    (((now() at time zone 'Asia/Seoul')::date + time '00:00') at time zone 'Asia/Seoul'),
    null
)
on conflict (session_id) do update set
    user_id = excluded.user_id,
    pet_id = excluded.pet_id,
    cage_id = excluded.cage_id,
    access_code = excluded.access_code,
    status = excluded.status,
    created_at = excluded.created_at,
    ended_at = excluded.ended_at;

-- A completed session on the previous day is intentionally sensor-only. It
-- verifies that raw SmartThings temperature/humidity readings generate an AI
-- report even when no pet_logs row exists for that date.
insert into public.access_session (
    session_id, user_id, pet_id, cage_id, access_code, status, created_at, ended_at
)
values (
    -900002,
    '11111111-1111-4111-8111-111111111111',
    '22222222-2222-4222-8222-222222222222',
    '33333333-3333-4333-8333-333333333333',
    'LOCAL-REPORT-SENSOR-ONLY',
    'ENDED',
    ((((now() at time zone 'Asia/Seoul')::date - 1) + time '00:00') at time zone 'Asia/Seoul'),
    ((((now() at time zone 'Asia/Seoul')::date - 1) + time '23:59') at time zone 'Asia/Seoul')
)
on conflict (session_id) do update set
    user_id = excluded.user_id,
    pet_id = excluded.pet_id,
    cage_id = excluded.cage_id,
    access_code = excluded.access_code,
    status = excluded.status,
    created_at = excluded.created_at,
    ended_at = excluded.ended_at;

insert into public.smartthings_device (
    smartthings_device_mapping_id,
    cage_id,
    smartthings_device_id,
    device_type,
    label,
    battery,
    online,
    active,
    last_seen_at,
    created_at,
    updated_at
)
values (
    '44444444-4444-4444-8444-444444444444',
    '33333333-3333-4333-8333-333333333333',
    'LOCAL-REPORT-TEMPERATURE-HUMIDITY',
    'TEMPERATURE_HUMIDITY',
    '로컬 온습도 센서',
    90,
    true,
    true,
    now(),
    now(),
    now()
)
on conflict (smartthings_device_mapping_id) do update set
    cage_id = excluded.cage_id,
    smartthings_device_id = excluded.smartthings_device_id,
    device_type = excluded.device_type,
    label = excluded.label,
    battery = excluded.battery,
    online = excluded.online,
    active = excluded.active,
    last_seen_at = excluded.last_seen_at,
    updated_at = excluded.updated_at;

delete from public.sensor_reading
where smartthings_device_mapping_id = '44444444-4444-4444-8444-444444444444';

insert into public.sensor_reading (
    smartthings_device_mapping_id,
    cage_id,
    session_id,
    capability,
    attribute,
    numeric_value,
    string_value,
    unit,
    measured_at,
    received_at,
    source,
    raw_payload
)
values
    (
        '44444444-4444-4444-8444-444444444444',
        '33333333-3333-4333-8333-333333333333',
        -900002,
        'temperatureMeasurement',
        'temperature',
        23.4,
        null,
        'C',
        ((((now() at time zone 'Asia/Seoul')::date - 1) + time '09:00') at time zone 'Asia/Seoul'),
        now(),
        'LOCAL_REPORT_DEMO',
        '{"source":"local-report-sensor-only"}'::jsonb
    ),
    (
        '44444444-4444-4444-8444-444444444444',
        '33333333-3333-4333-8333-333333333333',
        -900002,
        'relativeHumidityMeasurement',
        'humidity',
        56.2,
        null,
        '%',
        ((((now() at time zone 'Asia/Seoul')::date - 1) + time '09:00') at time zone 'Asia/Seoul'),
        now(),
        'LOCAL_REPORT_DEMO',
        '{"source":"local-report-sensor-only"}'::jsonb
    );

delete from public.pet_logs
where external_event_id like 'local-report-demo-%';

insert into public.pet_logs (
    session_id, external_event_id, log_type, data, created_at,
    event_ended_at, event_duration_seconds
)
values
    (
        -900001,
        'local-report-demo-sensor-1',
        'SENSOR',
        jsonb_build_object('message', '아침 환경 센서 수신', 'temperature', 24.8, 'humidity', 62.0),
        (((now() at time zone 'Asia/Seoul')::date + time '08:10') at time zone 'Asia/Seoul'),
        null,
        null
    ),
    (
        -900001,
        'local-report-demo-door-1',
        'DOOR_OPEN',
        jsonb_build_object('message', '아침 급여를 위한 케이지 문 열림'),
        (((now() at time zone 'Asia/Seoul')::date + time '08:30') at time zone 'Asia/Seoul'),
        (((now() at time zone 'Asia/Seoul')::date + time '08:31') at time zone 'Asia/Seoul'),
        60
    ),
    (
        -900001,
        'local-report-demo-rest-1',
        'RESTING',
        jsonb_build_object('message', '오전 안정적인 휴식 자세 관찰', 'confidence', 0.94),
        (((now() at time zone 'Asia/Seoul')::date + time '10:15') at time zone 'Asia/Seoul'),
        (((now() at time zone 'Asia/Seoul')::date + time '10:47') at time zone 'Asia/Seoul'),
        1920
    ),
    (
        -900001,
        'local-report-demo-sensor-2',
        'SENSOR',
        jsonb_build_object('message', '낮 환경 센서 수신', 'temperature', 25.6, 'humidity', 60.5),
        (((now() at time zone 'Asia/Seoul')::date + time '12:00') at time zone 'Asia/Seoul'),
        null,
        null
    ),
    (
        -900001,
        'local-report-demo-pacing-1',
        'PACING',
        jsonb_build_object('message', '문 앞을 반복해서 오가는 행동 관찰', 'confidence', 0.88),
        (((now() at time zone 'Asia/Seoul')::date + time '14:20') at time zone 'Asia/Seoul'),
        (((now() at time zone 'Asia/Seoul')::date + time '14:22') at time zone 'Asia/Seoul'),
        120
    ),
    (
        -900001,
        'local-report-demo-pacing-2',
        'PACING',
        jsonb_build_object('message', '짧은 반복 보행이 다시 관찰됨', 'confidence', 0.84),
        (((now() at time zone 'Asia/Seoul')::date + time '16:05') at time zone 'Asia/Seoul'),
        (((now() at time zone 'Asia/Seoul')::date + time '16:06') at time zone 'Asia/Seoul'),
        60
    ),
    (
        -900001,
        'local-report-demo-low-light-1',
        'LOW_LIGHT',
        jsonb_build_object('message', '해질 무렵 케이지 조도 저하 감지'),
        (((now() at time zone 'Asia/Seoul')::date + time '18:45') at time zone 'Asia/Seoul'),
        null,
        null
    ),
    (
        -900001,
        'local-report-demo-sensor-3',
        'SENSOR',
        jsonb_build_object('message', '저녁 환경 센서 수신', 'temperature', 25.9, 'humidity', 59.6),
        (((now() at time zone 'Asia/Seoul')::date + time '20:10') at time zone 'Asia/Seoul'),
        null,
        null
    );

delete from public.daily_report
where pet_id = '22222222-2222-4222-8222-222222222222'
  and report_date in (
      (now() at time zone 'Asia/Seoul')::date,
      (now() at time zone 'Asia/Seoul')::date - 1
  );

commit;

select
    'report.demo@peztz.local' as demo_email,
    '22222222-2222-4222-8222-222222222222'::uuid as demo_pet_id,
    (now() at time zone 'Asia/Seoul')::date as demo_report_date,
    count(*) as seeded_log_count
from public.pet_logs
where external_event_id like 'local-report-demo-%'
group by demo_email, demo_pet_id, demo_report_date;

select
    '22222222-2222-4222-8222-222222222222'::uuid as sensor_only_pet_id,
    (now() at time zone 'Asia/Seoul')::date - 1 as sensor_only_report_date,
    count(*) as seeded_sensor_reading_count
from public.sensor_reading
where smartthings_device_mapping_id = '44444444-4444-4444-8444-444444444444'
group by sensor_only_pet_id, sensor_only_report_date;
