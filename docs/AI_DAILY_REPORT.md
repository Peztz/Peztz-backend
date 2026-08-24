# AI 일일 리포트

## 결정된 동작

- 매일 `Asia/Seoul` 기준 00:10에 전날 리포트를 자동 생성합니다.
- 자동 생성이 빠졌다면 보호자가 조회할 때 즉시 보충 생성합니다.
- 같은 반려동물과 날짜의 리포트는 `daily_report`에 한 건만 저장합니다.
- 최초 동시 요청은 PostgreSQL 원자적 claim을 사용해 한 요청만 OpenAI를 호출합니다.
- 실패 리포트는 통계 카드와 함께 `FAILED`로 저장하고 10분 이후 조회 시 재시도합니다.
- `pet_logs`와 `sensor_reading`이 모두 없을 때만 OpenAI를 호출하지 않습니다.
- SmartThings 온습도만 있는 날짜도 평균 온습도를 집계해 AI 리포트를 생성합니다.
- 앱에는 마크다운 문자열 대신 구조화된 카드 JSON을 반환합니다.

## 데이터 흐름

```text
Spring scheduler 또는 보호자 GET 요청
  -> Spring이 보호자 권한과 petId 검증
  -> 짧은 읽기 트랜잭션으로 해당 날짜의 pet_logs와 sensor_reading 조회·집계
  -> PostgreSQL upsert로 GENERATING claim 획득
  -> 트랜잭션 밖에서 이름·품종·생년월일과 필요한 이벤트만 FastAPI에 전달
  -> FastAPI가 OpenAI Responses API Structured Outputs 호출
  -> 별도의 짧은 쓰기 트랜잭션으로 READY 또는 FAILED 저장
  -> 앱에 카드형 JSON 반환
```

FastAPI는 PostgreSQL에 직접 연결하지 않습니다. 이메일, 전화번호, 사용자 ID, 케이지
접근 코드와 원본 센서 JSON은 OpenAI에 전달하지 않습니다. 한 요청에 전달하는 관찰·온습도
이벤트는 합쳐서 최근 200건으로 제한하지만 전체 수와 환경 통계는 모든 당일 데이터를
기준으로 계산합니다. `totalLogCount`는 관찰 로그와 온습도 측정 건수의 합입니다.

## OpenAI 키 설정

키는 코드나 Git에 넣지 않습니다. 다음 예시 파일의 `CHANGE_ME`만 로컬 비밀 파일에서
교체합니다.

- Docker: `infra/.env.local.example`을 `infra/.env.local`로 복사
- FastAPI 단독 실행: `fastapi-stream-server/.env.example`을 `.env`로 복사

필수 값:

```text
OPENAI_API_KEY=CHANGE_ME_OPENAI_API_KEY
OPENAI_MODEL=gpt-5-mini
OPENAI_TIMEOUT_SECONDS=90
OPENAI_MAX_RETRIES=1
FASTAPI_REPORT_CONNECT_TIMEOUT_MILLIS=5000
FASTAPI_REPORT_READ_TIMEOUT_MILLIS=200000
```

구조화된 리포트는 일반 채팅보다 생성 시간이 길 수 있으므로 OpenAI 읽기 제한 시간은
기본 90초로 설정합니다. 일시적인 연결 실패는 한 번만 재시도합니다.
Spring의 FastAPI 읽기 제한 시간은 OpenAI 재시도 구간보다 긴 200초입니다.

OpenAI 키가 비어 있어도 Spring과 FastAPI는 시작됩니다. 이 경우 카메라 등 다른 기능은
사용할 수 있지만 AI 리포트는 `FAILED` 통계 카드로 반환됩니다.

## 스케줄 설정

```text
DAILY_REPORT_SCHEDULING_ENABLED=true
DAILY_REPORT_SCHEDULE_CRON=0 10 0 * * *
DAILY_REPORT_TIME_ZONE=Asia/Seoul
DAILY_REPORT_RETRY_AFTER_MINUTES=10
DAILY_REPORT_GENERATION_LEASE_SECONDS=210
DAILY_REPORT_GENERATION_WAIT_SECONDS=220
DAILY_REPORT_GENERATION_POLL_MILLIS=500
```

cron은 Spring의 6개 필드 형식 `초 분 시 일 월 요일`입니다. 기본값 `0 10 0 * * *`은
매일 00:10을 의미합니다. 자동 생성 대상은 전날 입실 세션과 시간이 겹치는 반려동물입니다.

## PostgreSQL

팀원이 관리하는 실제 PostgreSQL 연결과 실데이터 검증은
[팀 PostgreSQL E2E 가이드](TEAM_POSTGRES_E2E.md)를 따릅니다. 최초 실제 DB 검증에서는
자동 스케줄을 끄고 한 개의 승인된 반려동물·날짜만 수동으로 호출합니다.

빈 로컬 DB에는 다음 전체 스키마를 사용합니다.

```bash
psql -h 127.0.0.1 -p 15432 -U postgres -d PEZTZ \
  -f docs/sql/peztz_local_schema.sql
```

기존 운영 DB에는 전체 로컬 스키마를 실행하면 안 됩니다. 운영에는 다음 증분
마이그레이션만 적용합니다.

```bash
psql -h DB_HOST -U DB_USER -d PEZTZ \
  -f docs/sql/daily_report_migration.sql
```

Spring은 `ddl-auto=validate`이므로 운영 애플리케이션을 배포하기 전에 마이그레이션을
적용해야 합니다. 이 파일은 내부 상태 `GENERATING`과 원자적 생성 소유권에 사용하는
`generation_token`도 추가합니다.

## 로컬 Docker 실행

```bash
cd infra
cp .env.local.example .env.local
```

`.env.local`에서 DB 비밀번호, 세 개의 내부 API 키와 `OPENAI_API_KEY`를 채웁니다.
내부 키는 각각 다음 명령으로 생성할 수 있습니다.

```bash
openssl rand -hex 32
```

실행:

```bash
docker compose \
  --env-file .env.local \
  -f docker-compose.yml \
  -f docker-compose.local.yml \
  up -d --build postgres spring fastapi
```

확인:

```bash
docker compose \
  --env-file .env.local \
  -f docker-compose.yml \
  -f docker-compose.local.yml \
  ps

curl http://127.0.0.1:18080/actuator/health
curl http://127.0.0.1:18000/health
```

`peztz-postgres-local-data` 볼륨을 처음 만들 때만 전체 스키마가 자동 적용됩니다. 이미
볼륨을 사용 중이라면 스키마 파일을 수정해도 자동으로 다시 실행되지 않습니다.

## 로컬 테스트 데이터로 전체 흐름 검증

팀 PostgreSQL을 사용할 수 없는 동안에는 로컬 Docker PostgreSQL에만 테스트 전용
보호자, 반려동물, 입실 세션, 오늘 날짜 로그 8건과 전날의 온습도-only 측정 2건을 넣습니다.

```bash
cd /Users/jun/Documents/GitHub/Peztz-backend
./scripts/seed_local_daily_report_demo.sh
```

테스트 계정:

```text
email=report.demo@peztz.local
password=PeztzDemo!2026
petId=22222222-2222-4222-8222-222222222222
```

스크립트는 `infra/.env.local`의 로컬 DB 설정과 Docker Compose의 `postgres` 서비스만
사용합니다. 팀·공유 DB를 대상으로 실행하지 않으며, 반복 실행 시 고정된 테스트 데이터만
갱신합니다. 또한 오늘과 전날의 테스트 리포트를 삭제해 새 OpenAI 호출을 다시 검증할 수
있게 합니다. 전날을 선택하면 `pet_logs`가 전혀 없는 온습도-only 생성을 확인할 수 있습니다.

프론트엔드는 위 계정으로 로그인한 뒤 `/owner/reports`에서 오늘 날짜를 조회합니다.
CLI로 실제 OpenAI 호출까지 검증할 때는 로그인 응답의 토큰을 환경 변수로 전달합니다.

```bash
ACCESS_TOKEN='로그인으로 받은 토큰' \
PET_ID='22222222-2222-4222-8222-222222222222' \
REPORT_DATE="$(TZ=Asia/Seoul date +%F)" \
./scripts/verify_daily_report_e2e.sh
```

동시에 최초 요청 두 개를 보내 같은 `reportId`가 반환되는지 확인하려면 다음 스크립트를
사용합니다. FastAPI 접근 로그의 생성 API가 한 줄인지도 함께 확인합니다.

```bash
ACCESS_TOKEN='로그인으로 받은 토큰' \
PET_ID='22222222-2222-4222-8222-222222222222' \
REPORT_DATE="$(TZ=Asia/Seoul date -v-1d +%F)" \
./scripts/verify_daily_report_concurrency.sh
```

OpenAI 키를 아직 준비하지 못했더라도 구조화된 `READY` 응답과 프론트 카드 레이아웃은
다음 로컬 전용 모의 리포트로 검증할 수 있습니다.

```bash
./scripts/seed_local_daily_report_ready_mock.sh
```

이 스크립트는 OpenAI를 호출하지 않고 `model_name=local-ready-mock`인 결과 한 건을
`daily_report`에 저장합니다. 프론트는 실제 Spring API와 인증을 그대로 거치므로 화면
계약과 DB 재조회는 검증되지만 OpenAI 연동 성공 증거로 간주하면 안 됩니다. 실제 호출을
다시 검증할 때는 `seed_local_daily_report_demo.sh`를 재실행해 모의 리포트를 제거합니다.

`invalid_api_key`가 발생하면 `infra/.env.local`의 `OPENAI_API_KEY`를 유효한 OpenAI
Platform API 키로 교체하고 FastAPI 컨테이너만 다시 생성합니다. API 키는 명령줄이나
채팅에 출력하지 않습니다.

```bash
cd /Users/jun/Documents/GitHub/Peztz-backend/infra
docker compose --env-file .env.local \
  -f docker-compose.yml \
  -f docker-compose.local.yml \
  up -d --force-recreate fastapi
```

키를 바꾼 다음 seed 스크립트를 한 번 더 실행하면 이전 테스트 실패 리포트가 제거되므로
10분의 실패 재시도 대기 시간 없이 다시 검증할 수 있습니다.

## 보호자 API 응답

기존 API 경로를 유지합니다.

```http
GET /api/reports/daily?petId={petId}&date=YYYY-MM-DD
Authorization: Bearer {accessToken}
```

주요 카드 필드:

```json
{
  "status": "READY",
  "summary": "오늘은 전반적으로 안정적인 하루였습니다.",
  "behaviorCards": [
    {
      "title": "문 열림 감지",
      "description": "문 열림이 한 차례 감지되었습니다.",
      "evidence": ["09:10 DOOR_OPEN"]
    }
  ],
  "environmentCard": {
    "title": "생활 환경",
    "description": "온도와 습도가 안정적이었습니다.",
    "averageTemperature": 25.2,
    "averageHumidity": 61.0,
    "doorOpenCount": 1,
    "lowLightCount": 0
  },
  "careTips": ["물을 충분히 마시는지 확인해 주세요."],
  "riskLevel": "NORMAL",
  "warnings": [],
  "disclaimer": "이 리포트는 진단이 아닌 관찰 데이터 요약입니다."
}
```

`riskLevel`은 `NORMAL`, `ATTENTION`, `URGENT` 중 하나입니다. 이 값은 진단 결과가
아니며 앱은 항상 `disclaimer`를 함께 표시해야 합니다.

프론트엔드 구현자는 [일일 리포트 프론트엔드 인계 문서](FRONTEND_DAILY_REPORT_HANDOFF.md)의
TypeScript 계약과 화면 상태를 사용합니다. OpenAI 키와 프롬프트는 프론트엔드에 두지
않습니다.
