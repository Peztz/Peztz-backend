# 팀 PostgreSQL 실데이터 E2E 검증

## 검증 목표

테스트용 로컬 데이터가 아니라 팀원이 저장하고 있는 PostgreSQL의 실제 로그를 사용해
다음 경로 전체가 동작하는지 확인합니다.

```text
팀 PostgreSQL pet_logs
  -> Spring 권한 확인·날짜별 조회·통계 집계
  -> FastAPI 내부 인증 API
  -> OpenAI Responses API 구조화 출력
  -> Spring daily_report 저장
  -> 보호자 일일 리포트 API 응답
  -> 프론트엔드 카드 화면
```

실제 데이터 검증에서는 보호자 이메일, 전화번호, 사용자 ID, 케이지 접근 코드와 원본
센서 JSON 전체를 OpenAI에 전달하지 않습니다. Spring은 반려동물 기본 정보와 리포트에
필요한 통계·이벤트만 최대 200건 전달합니다.

## DB 담당자에게 받을 항목

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- 서버가 요구하는 `DB_SSLMODE` (`require`, `verify-full`, `prefer` 등)
- VPN, SSH 터널 또는 접속 허용 IP 등록 여부
- `public."Pets"`, `public.access_session`, `public.pet_logs` 스키마가 현재 코드와 같은지
- `daily_report` 증분 마이그레이션 적용 권한과 적용 담당자
- 실제 검증에 사용해도 되는 테스트 보호자 계정, `petId`, 로그가 존재하는 날짜

비밀번호, API 키와 로그인 토큰은 Git, 이슈, 메신저 본문 또는 문서에 넣지 않습니다.
채워진 `infra/.env.team`은 `.gitignore` 대상입니다.

## 1. 원격 DB 설정

```bash
cd /Users/jun/Documents/GitHub/Peztz-backend
cp infra/.env.team.example infra/.env.team
```

`infra/.env.team`에 전달받은 값을 입력합니다. 최초 검증이 끝날 때까지 아래 설정을
반드시 유지합니다.

```text
DAILY_REPORT_SCHEDULING_ENABLED=false
```

이 설정은 실제 DB의 여러 반려동물에 대해 의도치 않게 OpenAI 호출이 발생하는 것을
막습니다.

## 2. 읽기 전용 사전점검

현재 로컬 PostgreSQL 컨테이너를 사용하는 서비스가 실행 중이면 애플리케이션만 먼저
내립니다. `-v`를 붙이지 않으므로 로컬 DB 볼륨은 유지됩니다.

```bash
cd infra
docker compose --env-file .env.local \
  -f docker-compose.yml \
  -f docker-compose.local.yml \
  down
cd ..
```

원격 DB 환경변수를 현재 셸에만 불러온 뒤 사전점검을 실행합니다.

```bash
set -a
source infra/.env.team
set +a
./scripts/check_team_db.sh
```

이 스크립트는 `BEGIN READ ONLY`로 실행되며 행을 추가·수정·삭제하지 않습니다. 필수
테이블·컬럼, DB 시간대, 최근 7일 로그 수와 `daily_report` 마이그레이션 상태를 확인합니다.

## 3. 증분 마이그레이션

사전점검 결과가 `MIGRATION_REQUIRED`이면 DB 담당자가 다음 파일을 검토한 후 적용합니다.

```text
docs/sql/daily_report_migration.sql
```

운영/공용 DB에는 `peztz_local_schema.sql`을 실행하지 않습니다. `daily_report_migration.sql`은
기존 테이블을 다시 만들지 않고 리포트 저장 테이블과 인덱스만 추가합니다. Spring은
`ddl-auto=validate`이므로 이 마이그레이션이 없으면 시작을 거부하는 것이 정상입니다.

## 4. Spring과 FastAPI 실행

원격 DB에서는 `docker-compose.local.yml`을 사용하지 않습니다. 이 파일을 함께 지정하면
로컬 PostgreSQL로 연결됩니다.

```bash
cd infra
docker compose --env-file .env.team \
  -f docker-compose.yml \
  up -d --build spring fastapi

docker compose --env-file .env.team \
  -f docker-compose.yml \
  ps spring fastapi
```

상태 확인:

```bash
curl http://127.0.0.1:18080/actuator/health
curl http://127.0.0.1:18000/health
```

두 서비스가 모두 `healthy`여야 합니다. Spring이 시작하지 않으면 우선 다음 순서로
확인합니다.

1. VPN/IP 허용과 DB 호스트·포트
2. SSL 모드
3. DB 사용자 권한
4. Hibernate가 출력한 누락 테이블·컬럼
5. `daily_report` 마이그레이션 적용 여부

## 5. 실제 OpenAI E2E 호출

DB 담당자와 합의한 테스트 보호자 계정으로 로그인해 Bearer 토큰을 발급받습니다. 로그가
한 건 이상 존재하고 아직 `daily_report`가 없는 날짜를 고르면 신규 OpenAI 호출까지
명확히 확인할 수 있습니다.

토큰과 식별자는 현재 터미널 세션에만 설정합니다.

```bash
export ACCESS_TOKEN='발급받은_토큰'
export PET_ID='검증할_pet_uuid'
export REPORT_DATE='YYYY-MM-DD'
export SPRING_BASE_URL='http://127.0.0.1:18080'

./scripts/verify_daily_report_e2e.sh
```

스크립트는 다음을 자동 검증합니다.

- Spring health가 정상인지
- 인증된 사용자가 해당 반려동물에 접근할 수 있는지
- 선택한 날짜에 실제 로그가 존재하는지
- 응답이 구조화된 카드 스키마를 만족하는지
- AI 결과 상태가 `READY`인지
- 두 번째 조회가 같은 `reportId`를 반환해 DB 저장 결과를 재사용하는지

FastAPI 내부 호출 기록과 오류는 다음 명령으로 확인합니다.

```bash
cd infra
docker compose --env-file .env.team -f docker-compose.yml logs --since 10m fastapi
docker compose --env-file .env.team -f docker-compose.yml logs --since 10m spring
```

DB에서는 개인정보가 없는 메타데이터만 확인합니다.

```sql
select report_id,
       pet_id,
       report_date,
       status,
       total_log_count,
       model_name,
       generated_at
from public.daily_report
where pet_id = '검증한_pet_uuid'
  and report_date = date 'YYYY-MM-DD';
```

## 6. 자동 생성 활성화

수동 E2E가 성공하고 팀의 API 비용·데이터 사용 승인을 받은 뒤에만 다음 값을 바꿉니다.

```text
DAILY_REPORT_SCHEDULING_ENABLED=true
DAILY_REPORT_SCHEDULE_CRON="0 10 0 * * *"
DAILY_REPORT_TIME_ZONE=Asia/Seoul
```

서비스를 재생성하면 매일 00:10에 전날 입실 세션과 시간이 겹치는 반려동물의 리포트를
생성합니다.

```bash
docker compose --env-file .env.team -f docker-compose.yml up -d spring
```

## 판정 기준

완료 판정은 단순히 HTTP 200만 보는 것이 아닙니다.

- 팀 PostgreSQL에서 읽은 로그 수와 API의 `totalLogCount`가 일치합니다.
- FastAPI 로그에 내부 생성 API 요청이 기록됩니다.
- 응답이 `READY`이고 카드 배열과 환경 카드가 유효합니다.
- `daily_report`에 동일한 `reportId`가 저장됩니다.
- 두 번째 조회는 OpenAI를 다시 호출하지 않고 저장 결과를 반환합니다.
- 다른 사용자의 `petId` 조회는 노출 없이 `404`로 차단됩니다.
- 프론트엔드는 같은 JSON을 별도 가공 없이 카드에 표시합니다.

