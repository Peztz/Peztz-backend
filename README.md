# Peztz Backend

Peztz는 보호자가 반려동물의 입실 상태, 카메라 이벤트와 케이지 환경을 확인할 수 있게
하는 모니터링 백엔드입니다. Spring Boot가 인증·도메인·PostgreSQL을 담당하고,
FastAPI가 MediaMTX 및 OpenAI 같은 내부 연동을 담당합니다.

## 핵심 기능

- 보호자 회원가입·로그인과 Bearer 토큰 인증
- 반려동물, 시설, 케이지, 입실 세션 관리
- Raspberry Pi 및 Tapo/MediaMTX 카메라 상태 연동
- 이벤트, 온도·습도·문 열림·조도 로그 저장
- SmartThings 센서 동기화
- PostgreSQL 로그 기반 AI 일일 리포트 생성·저장
- 프론트엔드가 바로 렌더링할 수 있는 구조화 카드 JSON 제공

## 기술 구성

| 구성요소 | 기술 | 역할 |
| --- | --- | --- |
| `spring-server` | Java 21, Spring Boot, JPA/Hibernate | 인증, 권한, 도메인 API, DB 집계·저장, 스케줄 |
| `fastapi-stream-server` | Python 3.12, FastAPI, OpenAI SDK | 내부 AI 생성 API, MediaMTX 상태, 장치 이벤트 전달 |
| PostgreSQL | PostgreSQL 16 기준 | 사용자·반려동물·세션·로그·리포트 저장 |
| `raspberry-pi` | Python, FFmpeg, systemd | 장치 등록과 Tapo RTSP 송출 |
| Docker Compose | Docker Desktop/Engine | 로컬 및 서버 실행 |

## 시스템 구조

```text
보호자 앱
  -> Spring Boot 공개 API
       -> PostgreSQL
       -> FastAPI 내부 API
            -> OpenAI Responses API
            -> MediaMTX Control API

Raspberry Pi / SmartThings
  -> FastAPI 또는 Spring 내부 API
       -> PostgreSQL pet_logs
```

외부 클라이언트는 Spring 공개 API만 호출합니다. FastAPI 내부 리포트 API와 OpenAI 키,
DB 비밀번호는 프론트엔드에 공개하지 않습니다.

## AI 일일 리포트 흐름

```text
매일 00:10 스케줄 또는 보호자 조회
  -> Spring이 사용자와 petId 소유권 확인
  -> Asia/Seoul 기준 해당 날짜의 pet_logs 조회
  -> 전체 로그 통계 집계, 최근 이벤트 최대 200건 선별
  -> FastAPI에 최소 데이터 전달
  -> OpenAI Responses API Structured Outputs 호출
  -> daily_report JSONB 저장
  -> 구조화 카드 응답 반환
```

- 매일 00:10에 전날 리포트를 생성합니다.
- 생성이 누락된 날짜는 첫 조회 때 보충 생성합니다.
- 같은 반려동물·날짜는 한 건만 저장합니다.
- 로그 수가 바뀌면 늦게 들어온 데이터를 포함해 다시 생성합니다.
- 로그가 없으면 OpenAI를 호출하지 않고 데이터 부족 카드를 저장합니다.
- AI 호출이 실패해도 DB 통계는 유지하며 `FAILED` 상태와 재시도 안내를 반환합니다.
- 이메일, 전화번호, 사용자 ID, 접근 코드와 원본 센서 JSON 전체는 OpenAI로 보내지 않습니다.

OpenAI 호출은 Responses API의 서버 측 instructions와 구조화 출력을 사용하며 응답 저장은
`store=false`로 요청합니다. 프롬프트는 `fastapi-stream-server/report_prompt.py`에서 버전
관리합니다. 참고: [OpenAI Responses API](https://developers.openai.com/api/reference/cli/resources/responses/methods/create)

## 저장소 구조

```text
Peztz-backend/
├── spring-server/             # 공개 REST API와 PostgreSQL 도메인
├── fastapi-stream-server/     # 내부 AI·MediaMTX·장치 연동 API
├── raspberry-pi/              # 장치 등록과 카메라 송출 서비스
├── infra/                     # Docker Compose와 환경변수 예시
├── docs/                      # API, AI, 센서, DB 마이그레이션 문서
└── scripts/                   # 팀 DB 사전점검과 E2E 검증 도구
```

## 로컬 실행

### 요구사항

- macOS 또는 Linux
- Docker Desktop/Engine과 Docker Compose
- Git
- 직접 실행·개발 시 Java 21, Python 3.12, `psql`
- 실제 AI 생성 시 OpenAI Platform API 키와 사용 가능한 API 크레딧

### 비밀 설정

```bash
cd /Users/jun/Documents/GitHub/Peztz-backend
cp infra/.env.local.example infra/.env.local
```

다음 값을 실제 값으로 교체합니다.

- `DB_PASSWORD`
- `PEZTZ_INTERNAL_API_KEY`
- `FASTAPI_INTERNAL_API_KEY`
- `DEVICE_API_KEY`
- `OPENAI_API_KEY`

세 내부 키는 서로 다른 값이어야 하며 다음 명령으로 생성할 수 있습니다.

```bash
openssl rand -hex 32
```

채워진 `.env.local`, `.env.team`, `.env`는 Git 추적 대상이 아닙니다.

### 시작

```bash
cd infra
docker compose --env-file .env.local \
  -f docker-compose.yml \
  -f docker-compose.local.yml \
  up -d --build postgres spring fastapi
```

### 상태 확인

```bash
docker compose --env-file .env.local \
  -f docker-compose.yml \
  -f docker-compose.local.yml \
  ps

curl http://127.0.0.1:18080/actuator/health
curl http://127.0.0.1:18000/health
```

- Spring Swagger UI: `http://127.0.0.1:18080/swagger-ui/index.html`
- Spring health: `http://127.0.0.1:18080/actuator/health`
- FastAPI health: `http://127.0.0.1:18000/health`

### 종료

```bash
docker compose --env-file .env.local \
  -f docker-compose.yml \
  -f docker-compose.local.yml \
  down
```

DB를 유지하려면 `down -v`를 사용하지 않습니다.

## 팀 PostgreSQL 실데이터 연결

실제 팀 DB에는 전체 로컬 스키마를 적용하지 않습니다. 다음 순서를 사용합니다.

1. `infra/.env.team.example`을 `infra/.env.team`으로 복사합니다.
2. DB 담당자에게 접속 정보, SSL 모드, VPN/IP 허용 여부를 받습니다.
3. `scripts/check_team_db.sh`로 읽기 전용 사전점검을 수행합니다.
4. DB 담당자가 `docs/sql/daily_report_migration.sql`을 검토·적용합니다.
5. 자동 스케줄을 끈 상태로 승인된 한 개 pet/date를 E2E 검증합니다.
6. 검증과 비용 승인이 끝나면 자동 생성을 켭니다.

명령과 완료 판정 기준은 [팀 PostgreSQL E2E 문서](docs/TEAM_POSTGRES_E2E.md)에 있습니다.

## 주요 API

### 인증

```text
POST /api/auth/signup
POST /api/auth/login
GET  /api/auth/me
```

### 반려동물·입실·로그

```text
GET  /api/pets/my
POST /api/admission-sessions
GET  /api/admission-sessions/{sessionId}/logs
POST /api/admission-sessions/{sessionId}/logs
```

### 일일 리포트

```http
GET /api/reports/daily?petId={petId}&date=YYYY-MM-DD
Authorization: Bearer {accessToken}
```

또는 입실 세션 기준으로 조회합니다.

```http
GET /api/admission-sessions/{sessionId}/daily-report?date=YYYY-MM-DD
Authorization: Bearer {accessToken}
```

응답의 핵심 필드는 `summary`, `behaviorCards`, `environmentCard`, `careTips`,
`riskLevel`, `warnings`, `disclaimer`입니다. 프론트는 LLM 원문이나 마크다운을 파싱하지
않고 이 필드를 카드 컴포넌트에 직접 연결합니다.

## 프론트엔드 연동 원칙

- OpenAI 호출과 프롬프트는 FastAPI 서버에만 둡니다.
- OpenAI API 키를 프론트 환경변수, 번들, 네트워크 요청에 넣지 않습니다.
- 프론트는 Spring의 인증된 리포트 API만 호출합니다.
- `FAILED` 상태에서도 DB 통계 카드를 유지하고 AI 재시도 안내를 표시합니다.
- `disclaimer`는 항상 화면 하단에 표시합니다.
- 프론트 개발·배포 URL은 `CORS_ALLOWED_ORIGINS`에 명시적으로 등록합니다.

타입, 화면 상태와 인계 체크리스트는
[프론트엔드 일일 리포트 계약](docs/FRONTEND_DAILY_REPORT_HANDOFF.md)을 사용합니다.

## 테스트와 품질 확인

### Spring

```bash
cd spring-server
./gradlew clean test --no-daemon
```

### FastAPI

```bash
cd fastapi-stream-server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt -r requirements-dev.txt
pytest -q
```

### Compose 설정

```bash
cd infra
docker compose --env-file .env.local \
  -f docker-compose.yml \
  -f docker-compose.local.yml \
  config --quiet
```

### 실제 데이터 E2E

```bash
ACCESS_TOKEN='...' \
PET_ID='...' \
REPORT_DATE='YYYY-MM-DD' \
./scripts/verify_daily_report_e2e.sh
```

단위 테스트 성공만으로 실제 OpenAI E2E가 완료된 것은 아닙니다. 최종 배포 전에는 팀
PostgreSQL에서 로그 조회, OpenAI 생성, `daily_report` 저장, 두 번째 조회의 저장 결과
재사용과 프론트 렌더링까지 확인합니다.

## 운영 설정

| 환경변수 | 설명 | 기본/권장값 |
| --- | --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | PostgreSQL 위치 | 환경별 설정 |
| `DB_SSLMODE` | PostgreSQL SSL 정책 | 원격 DB `require` 이상 권장 |
| `CORS_ALLOWED_ORIGINS` | 허용 프론트 URL 목록 | 쉼표 구분, 와일드카드 금지 |
| `OPENAI_API_KEY` | 서버 전용 OpenAI 키 | 필수, Git 금지 |
| `OPENAI_MODEL` | 리포트 모델 | `gpt-5-mini` |
| `DAILY_REPORT_SCHEDULING_ENABLED` | 자동 생성 활성화 | 최초 E2E `false`, 승인 후 `true` |
| `DAILY_REPORT_SCHEDULE_CRON` | Spring 6필드 cron | `0 10 0 * * *` |
| `DAILY_REPORT_TIME_ZONE` | 집계·스케줄 기준 | `Asia/Seoul` |

전체 Docker 운영 절차는 [infra README](infra/README.md)를 참고합니다.

## 보안 원칙

- DB·OpenAI·장치·내부 API 키를 저장소에 커밋하지 않습니다.
- 사용자 소유권을 확인한 뒤에만 반려동물 리포트를 반환합니다.
- Spring과 FastAPI 내부 통신도 독립된 API 키로 인증합니다.
- 실제 데이터 E2E는 팀이 승인한 테스트 반려동물과 날짜로 제한합니다.
- 프론트에는 공개 API URL과 Bearer 토큰만 사용하며 서버 비밀을 포함하지 않습니다.
- 운영 DB 마이그레이션과 데이터 삭제는 DB 담당자 승인 없이 수행하지 않습니다.

## 문서

- [AI 일일 리포트 설계·실행](docs/AI_DAILY_REPORT.md)
- [팀 PostgreSQL 실데이터 E2E](docs/TEAM_POSTGRES_E2E.md)
- [프론트엔드 일일 리포트 인계](docs/FRONTEND_DAILY_REPORT_HANDOFF.md)
- [전체 프론트엔드 API 가이드](docs/FRONTEND_API_GUIDE.md)
- [SmartThings 센서 연동](docs/SMARTTHINGS_SENSOR_INTEGRATION.md)
- [Docker 운영](infra/README.md)
- [Raspberry Pi 카메라 송출](raspberry-pi/README.md)
