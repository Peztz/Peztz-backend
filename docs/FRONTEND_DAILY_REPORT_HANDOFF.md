# 프론트엔드 일일 리포트 연동 계약

## 책임 분리

프론트엔드는 OpenAI를 직접 호출하거나 프롬프트·OpenAI API 키를 보관하지 않습니다.

| 영역 | 책임 |
| --- | --- |
| Spring | 로그인 권한, PostgreSQL 조회·집계, 리포트 저장, 공개 API |
| FastAPI | 서버 내부 프롬프트, OpenAI 호출, 구조화 출력 검증 |
| PostgreSQL | 원본 로그와 생성된 `daily_report` 영속화 |
| 프론트엔드 | 날짜 선택, 로딩·오류 상태, 카드형 결과 렌더링 |

현재 프롬프트는 `fastapi-stream-server/report_prompt.py`의 버전 관리되는 서버 코드에
있습니다. 일반 보호자 앱에
프롬프트 입력창을 만들지 않습니다. 프롬프트를 제품 운영자가 수정해야 하는 요구가 생기면
관리자 전용 권한, 버전, 검토·배포 상태와 감사 로그를 갖춘 별도 기능으로 설계합니다.

## 호출 API

```http
GET /api/reports/daily?petId={petId}&date=YYYY-MM-DD
Authorization: Bearer {accessToken}
```

입실 세션 화면에서 호출할 경우 다음 경로도 같은 응답을 반환합니다.

```http
GET /api/admission-sessions/{sessionId}/daily-report?date=YYYY-MM-DD
Authorization: Bearer {accessToken}
```

첫 조회에서 저장된 결과가 없으면 생성까지 최대 수십 초가 걸릴 수 있으므로 스켈레톤과
명시적인 생성 중 문구를 표시합니다. 이후 조회는 DB 결과를 반환합니다.

## TypeScript 계약

```ts
export type ReportStatus = "READY" | "FAILED";
export type RiskLevel = "NORMAL" | "ATTENTION" | "URGENT";

export interface BehaviorCard {
  title: string;
  description: string;
  evidence: string[];
}

export interface EnvironmentCard {
  title: string;
  description: string;
  averageTemperature: number | null;
  averageHumidity: number | null;
  doorOpenCount: number;
  lowLightCount: number;
}

export interface DailyReport {
  reportId: string;
  petId: string;
  petName: string;
  date: string;
  status: ReportStatus;
  totalLogCount: number;
  sensorLogCount: number;
  averageTemperature: number | null;
  averageHumidity: number | null;
  summary: string;
  behaviorCards: BehaviorCard[];
  environmentCard: EnvironmentCard;
  careTips: string[];
  riskLevel: RiskLevel;
  warnings: string[];
  disclaimer: string;
  generatedAt: string;
}
```

## 화면 상태

| 조건 | 화면 동작 |
| --- | --- |
| 요청 중 | 카드 스켈레톤과 `리포트를 생성하고 있어요` 표시 |
| `READY`, 로그 있음 | 요약·행동·환경·돌봄 팁 카드 표시 |
| `READY`, `totalLogCount === 0` | 데이터 부족 안내와 센서 연결 확인 팁 표시 |
| `FAILED` | 수집 통계는 유지하고 AI 분석 재시도 안내 표시 |
| HTTP `401` | 로그인 갱신 또는 로그인 화면 이동 |
| HTTP `404` | 반려동물/세션 접근 불가 화면 |
| 네트워크/5xx | 재시도 버튼과 비파괴적 오류 메시지 |

`FAILED`는 HTTP 실패가 아니라 저장된 리포트 상태입니다. 이때도 DB에서 계산된 온도,
습도와 로그 수를 숨기지 않습니다. `disclaimer`는 모든 성공 리포트의 하단에 항상
노출합니다.

## 권장 카드 배치

1. 날짜와 반려동물 이름
2. `riskLevel` 배지와 `summary`
3. 환경 카드
4. `behaviorCards` 반복 목록
5. `careTips` 체크리스트
6. `warnings`
7. `generatedAt`과 `disclaimer`

프론트는 마크다운이나 LLM 원문을 파싱하지 않습니다. API의 각 구조화 필드를 지정된
컴포넌트에 직접 연결합니다.

## 프론트 저장소를 받은 뒤 진행할 작업

1. 사용 프레임워크, 라우팅, 상태관리와 기존 API 클라이언트를 확인합니다.
2. 위 타입과 API 함수를 프론트 공용 API 계층에 추가합니다.
3. 기존 인증 토큰 주입 방식을 그대로 사용합니다.
4. 일일 리포트 페이지에 로딩·정상·데이터 없음·실패 UI를 구현합니다.
5. `CORS_ALLOWED_ORIGINS`에 프론트 개발 및 배포 주소를 추가합니다.
6. 목 응답 컴포넌트 테스트와 실제 백엔드 E2E를 모두 수행합니다.
7. 프론트 환경변수에는 백엔드 공개 URL만 넣고 OpenAI 키는 절대 넣지 않습니다.
