-- Local-only READY report used to verify the frontend card contract without OpenAI.
-- Run through scripts/seed_local_daily_report_ready_mock.sh only after the demo seed.

begin;

do $$
begin
    if not exists (
        select 1
        from public."Pets"
        where pet_id = '22222222-2222-4222-8222-222222222222'
    ) then
        raise exception 'Local demo pet is missing. Run scripts/seed_local_daily_report_demo.sh first.';
    end if;
end $$;

insert into public.daily_report (
    report_id,
    pet_id,
    report_date,
    status,
    total_log_count,
    sensor_log_count,
    average_temperature,
    average_humidity,
    door_open_count,
    low_light_count,
    content,
    model_name,
    error_message,
    generated_at,
    created_at,
    updated_at
)
values (
    '44444444-4444-4444-8444-444444444444',
    '22222222-2222-4222-8222-222222222222',
    (now() at time zone 'Asia/Seoul')::date,
    'READY',
    8,
    3,
    25.4333333333,
    60.7,
    1,
    1,
    jsonb_build_object(
        'summary', '초코는 휴식 시간을 안정적으로 보냈고, 오후에 짧은 반복 보행이 두 차례 관찰되었습니다.',
        'behaviorCards', jsonb_build_array(
            jsonb_build_object(
                'title', '안정적인 오전 휴식',
                'description', '오전에는 약 32분 동안 편안한 휴식 자세가 관찰되었습니다.',
                'evidence', jsonb_build_array('10:15~10:47 RESTING · 32분', '관찰 신뢰도 94%')
            ),
            jsonb_build_object(
                'title', '짧은 반복 보행',
                'description', '오후에 문 앞을 오가는 행동이 두 차례 있었지만 각 관찰 시간은 짧았습니다.',
                'evidence', jsonb_build_array('14:20~14:22 PACING · 2분', '16:05~16:06 PACING · 1분')
            )
        ),
        'environmentCard', jsonb_build_object(
            'title', '대체로 안정적인 생활 환경',
            'description', '세 차례 측정된 온도와 습도는 큰 변동 없이 유지되었고, 저녁에 저조도가 한 차례 감지되었습니다.'
        ),
        'careTips', jsonb_build_array(
            '반복 보행이 같은 시간대에 이어지는지 내일도 관찰해 주세요.',
            '저녁에는 케이지 내부 조명이 충분한지 확인해 주세요.'
        ),
        'riskLevel', 'ATTENTION',
        'warnings', jsonb_build_array('반복 보행은 진단이 아니며, 지속되거나 다른 증상이 동반되면 전문가와 상담해 주세요.'),
        'disclaimer', '이 리포트는 진단이 아닌 관찰 데이터 요약입니다.'
    ),
    'local-ready-mock',
    null,
    now(),
    now(),
    now()
)
on conflict (pet_id, report_date) do update set
    status = excluded.status,
    total_log_count = excluded.total_log_count,
    sensor_log_count = excluded.sensor_log_count,
    average_temperature = excluded.average_temperature,
    average_humidity = excluded.average_humidity,
    door_open_count = excluded.door_open_count,
    low_light_count = excluded.low_light_count,
    content = excluded.content,
    model_name = excluded.model_name,
    error_message = excluded.error_message,
    generated_at = excluded.generated_at,
    updated_at = excluded.updated_at;

commit;

select
    report_id,
    report_date,
    status,
    total_log_count,
    model_name
from public.daily_report
where pet_id = '22222222-2222-4222-8222-222222222222'
  and report_date = (now() at time zone 'Asia/Seoul')::date;
