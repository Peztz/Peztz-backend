# Peztz 프론트엔드 API 연동 가이드

## 문서 범위

이 문서는 웹·앱 프론트엔드가 호출하는 **공개 Spring Boot API**만 설명합니다.

- Raspberry Pi의 내부 IP, RTSP 주소·계정·비밀번호는 프론트엔드에서 사용하지 않습니다.
- FastAPI → Spring 내부 이벤트 전송 API와 DB 마이그레이션은 이 문서의 범위가 아닙니다.
- 영상 분석, RTSP 수신, 클립 생성, GCP Storage 업로드는 Raspberry Pi/FastAPI 서비스가 담당합니다.

## 시스템 흐름

```text
Tapo C225 stream2
  → Raspberry Pi/FastAPI의 YOLO 분석
  → 이벤트 클립·썸네일 생성 및 GCP Storage 업로드
  → Spring 내부 API로 이벤트 메타데이터 전송
  → 프론트엔드는 Spring 공개 API로 카메라·이벤트를 조회
```

평상시 저화질 분석 영상과 고화질 원본 영상은 프론트엔드로 계속 전송하지 않습니다. 실시간 고화질 보기(MediaMTX)는 다음 단계 기능이며, 현재 공개 API로는 실제 재생을 시작할 수 없습니다.

## 서버 주소와 인증

프론트엔드는 배포 환경변수로 API 주소를 관리합니다. 실제 서버 IP나 포트를 소스 코드에 하드코딩하지 않습니다.

```text
API_BASE_URL=https://api.example.com
```

로그인 이후에는 응답의 `accessToken`을 사용합니다. 현재 토큰은 JWT가 아니라 서버 DB에서 관리하는 Bearer 토큰입니다.

```http
Authorization: Bearer {accessToken}
```

성공 응답은 각 API의 JSON 객체 또는 배열을 직접 반환합니다. 오류 응답은 다음 형식입니다.

```json
{
  "timestamp": "2026-07-28T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Pet not found"
}
```

프론트는 상태 코드에 따라 아래처럼 처리합니다.

| 상태 | 프론트 처리 기준 |
| --- | --- |
| `400` | 입력값 또는 요청 형식 오류를 사용자에게 안내 |
| `401` | 토큰 삭제 후 로그인 화면으로 이동 |
| `404` | 삭제됐거나 권한 없는 리소스로 처리. 다른 사용자의 리소스 존재 여부를 추측하지 않음 |
| `409` | 중복 등록 또는 현재 상태와 충돌한 요청으로 처리 |
| `500` | 일반적인 서버 오류 화면·재시도 UI 표시. 원문 오류를 사용자에게 그대로 노출하지 않음 |

## 인증 API

### 회원가입

```http
POST /api/auth/signup
Content-Type: application/json

{
  "email": "owner@example.com",
  "password": "password1234",
  "name": "보호자",
  "phoneNumber": "010-1234-5678",
  "role": "OWNER"
}
```

### 로그인

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "owner@example.com",
  "password": "password1234"
}
```

```json
{
  "accessToken": "sample-token",
  "user": {
    "id": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
    "email": "owner@example.com",
    "name": "보호자",
    "role": "OWNER"
  }
}
```

### 내 정보

```http
GET /api/auth/me
Authorization: Bearer {accessToken}
```

## 반려동물 API

보호자는 자신의 반려동물만 조회·수정·삭제할 수 있습니다.

```text
POST   /api/pets
GET    /api/pets/my
GET    /api/pets/{petId}
PUT    /api/pets/{petId}
DELETE /api/pets/{petId}
```

반려동물 등록·수정 요청 예시입니다.

```json
{
  "name": "초코",
  "species": "DOG",
  "breed": "Poodle",
  "gender": "MALE",
  "birthDate": "2022-03-01",
  "weightKg": 5.2,
  "memo": "낯선 사람을 조심함"
}
```

## 시설·Cage·입실 세션 API

시설 관리 화면과 입실 흐름에서 사용하는 API입니다. 시설 관리 API는 사용자 역할에 따라 접근이 제한될 수 있습니다.

```text
GET    /api/facilities
POST   /api/facilities
GET    /api/facilities/{facilityId}/cages
POST   /api/facilities/{facilityId}/cages

GET    /api/cages
GET    /api/cages/{cageId}
PUT    /api/cages/{cageId}
DELETE /api/cages/{cageId}

POST   /api/admission-sessions
GET    /api/admission-sessions/my
GET    /api/admission-sessions/{sessionId}
PATCH  /api/admission-sessions/{sessionId}/end
POST   /api/admission-sessions/access-code/verify
GET    /api/owners/me/cages
```

입실 세션 생성 요청입니다.

```json
{
  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23"
}
```

세션이 `ACTIVE` 상태이면 Cage에는 현재 사용자와 현재 반려동물이 연결됩니다. FastAPI가 보내는 이상행동 이벤트도 이 활성 세션을 기준으로 검증·저장됩니다.

### 실시간 영상 보기

기존 Raspberry Pi Camera Module 기반 MJPEG 프록시와 Cage·입실 세션 응답의 `videoUrl` 필드는 제거되었습니다. 새 Tapo 카메라의 실시간 영상 보기는 MediaMTX 기반 기능이 배포되기 전까지 화면에서 준비 중으로 표시하세요.

## 세션 로그·리포트 API

```text
GET  /api/admission-sessions/{sessionId}/logs
POST /api/admission-sessions/{sessionId}/logs

GET  /api/reports/daily?petId={petId}&date=YYYY-MM-DD
GET  /api/admission-sessions/{sessionId}/daily-report?date=YYYY-MM-DD
```

일반 세션 로그 등록 요청 예시입니다.

```json
{
  "type": "SENSOR",
  "message": "센서 데이터 수신",
  "temperature": 25.4,
  "humidity": 61.2
}
```

## 카메라 API

카메라는 Cage당 한 대만 등록할 수 있습니다. 카메라가 연결된 Cage의 현재 사용자만 카메라를 조회·수정할 수 있습니다.

```text
POST /api/cameras
GET  /api/cameras/my
GET  /api/cameras/{cameraId}
PUT  /api/cameras/{cameraId}
GET  /api/cameras/{cameraId}/runtime-status
```

### 카메라 등록

```http
POST /api/cameras
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23",
  "name": "거실 카메라"
}
```

일반 보호자 프론트는 `rtspConfigKey`를 보내지 않습니다. 이 값은 Raspberry Pi 또는 시크릿 저장소의 RTSP 설정을 가리키는 장비 프로비저닝용 선택 필드이며, RTSP URL·카메라 계정·비밀번호를 넣거나 프론트엔드에 저장하면 안 됩니다.

```json
{
  "cameraId": "c4eebef5-98f0-41b4-b6e0-bc7e0dc1f97e",
  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23",
  "cageName": "A-1 케이지",
  "name": "거실 카메라",
  "status": "REGISTERED",
  "streamStatus": "IDLE",
  "rtspConfigured": true,
  "createdAt": "2026-07-28T01:15:30Z",
  "updatedAt": "2026-07-28T01:15:30Z"
}
```

응답에는 RTSP URL·계정·비밀번호·`rtspConfigKey`가 포함되지 않습니다.

### 카메라 API 계약

모든 카메라 API에는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

| 화면 기능 | 메서드·경로 | 요청 본문 | 성공 응답 | 주요 오류 |
| --- | --- | --- | --- | --- |
| 카메라 등록 | `POST /api/cameras` | `cageId`, `name` | `CameraResponse` | `400`, `401`, `404`, `409` |
| 내 카메라 목록 | `GET /api/cameras/my` | 없음 | `CameraResponse[]` | `401` |
| 카메라 상세 | `GET /api/cameras/{cameraId}` | 없음 | `CameraResponse` | `401`, `404` |
| 카메라 이름 수정 | `PUT /api/cameras/{cameraId}` | `name` | `CameraResponse` | `400`, `401`, `404` |
| 런타임 상태 | `GET /api/cameras/{cameraId}/runtime-status` | 없음 | `CameraRuntimeStatusResponse` | `401`, `404` |

`CameraResponse` 필드입니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `cameraId` | UUID | 카메라 식별자 |
| `cageId` | UUID | 연결된 Cage 식별자 |
| `cageName` | string | Cage 표시 이름 |
| `name` | string | 카메라 표시 이름 |
| `status` | string | 등록 상태 |
| `streamStatus` | string | 송출 상태 |
| `rtspConfigured` | boolean | 서버 측 RTSP 설정 참조 존재 여부 |
| `createdAt`, `updatedAt` | ISO-8601 datetime | 생성·수정 시각 |

### 런타임 상태

```http
GET /api/cameras/{cameraId}/runtime-status
Authorization: Bearer {accessToken}
```

로컬 개발의 기본 설정은 Mock 상태를 반환합니다. GCP에서 `FASTAPI_CLIENT_MODE=http`으로 배포하면 FastAPI가 MediaMTX Control API를 조회하여 실제 상태와 재생 URL을 반환합니다. 라즈베리파이가 송출하지 않거나 오프라인이면 `playbackUrl`은 `null`입니다.

```json
{
  "cameraId": "c4eebef5-98f0-41b4-b6e0-bc7e0dc1f97e",
  "status": "ONLINE",
  "playbackUrl": "http://34.50.7.78:8889/cage-a1/",
  "message": "Camera stream is online"
}
```

## 이상행동 이벤트 API

프론트엔드는 보호자용 조회 API만 사용합니다. 이벤트 생성은 Raspberry Pi/FastAPI만 호출할 수 있는 내부 API이므로 프론트에서 호출하거나 내부 API 키를 보관하면 안 됩니다.

```text
GET /api/pet-events/my
GET /api/pet-events/my?petId={petId}
GET /api/pet-events/{eventId}
```

`eventType`은 `pet_logs.log_type`에 실제 행동 분류값으로 저장됩니다. 예를 들어
`EXCESSIVE_BARKING`, `FALL`, `VOMITING` 같은 값입니다. `externalEventId`는 행동
분류가 아니라 감지 장치가 생성한 이벤트 고유번호이며, 동일 이벤트가 재전송될 때
중복 저장을 막는 용도로 사용됩니다. 신뢰도와 추가 분석 정보는 `pet_logs.data`에
JSON으로 저장됩니다.

- `eventId`는 UUID가 아닌 숫자형 ID입니다.
- 목록과 상세는 로그인한 보호자가 소유한 반려동물의 이벤트만 반환합니다.
- `404`는 이벤트가 없거나 다른 사용자의 이벤트에 접근한 경우에 반환될 수 있습니다.

모든 이벤트 조회 API에는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

| 화면 기능 | 메서드·경로 | 쿼리 | 성공 응답 | 주요 오류 |
| --- | --- | --- | --- | --- |
| 전체 이벤트 목록 | `GET /api/pet-events/my` | 없음 | `PetEventResponse[]` | `401` |
| 반려동물별 이벤트 목록 | `GET /api/pet-events/my` | `petId` (UUID) | `PetEventResponse[]` | `401`, `404` |
| 이벤트 상세 | `GET /api/pet-events/{eventId}` | 없음 | `PetEventResponse` | `401`, `404` |

현재 이벤트 목록 API에는 페이지네이션·기간·이벤트 타입 필터가 없습니다. 프론트는 우선 응답 배열 전체를 표시하고, 데이터가 많아지면 백엔드 필터 API를 별도 추가합니다.

응답 예시입니다.

```json
{
  "eventId": 42,
  "externalEventId": "camera-001-20260728-0001",
  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
  "petName": "초코",
  "cameraId": "c4eebef5-98f0-41b4-b6e0-bc7e0dc1f97e",
  "cameraName": "거실 카메라",
  "eventType": "EXCESSIVE_BARKING",
  "confidence": 0.93,
  "createdAt": "2026-07-28T01:15:30Z",
  "eventEndedAt": "2026-07-28T01:16:00Z",
  "eventDurationSeconds": 30,
  "clipStartAt": "2026-07-28T01:15:20Z",
  "clipEndAt": "2026-07-28T01:16:10Z",
  "clipDurationSeconds": 50,
  "videoUrl": "https://storage.example.com/events/event-001.mp4",
  "thumbnailUrl": "https://storage.example.com/thumbnails/event-001.jpg",
  "metadata": {
    "model": "yolo-v1"
  }
}
```

### 이벤트 시간 의미

| 필드 | 의미 |
| --- | --- |
| `createdAt` | 이상행동이 시작되었거나 감지된 시각 |
| `eventEndedAt` | 이상행동이 종료된 시각 |
| `eventDurationSeconds` | 이상행동 지속 시간(초) |
| `clipStartAt` | 전후 버퍼를 포함한 증거 영상 시작 시각 |
| `clipEndAt` | 증거 영상 종료 시각 |
| `clipDurationSeconds` | 증거 영상 전체 길이(초) |

예를 들어 행동이 5초만 지속됐더라도, 전후 버퍼가 포함된 증거 영상은 25초일 수 있습니다.

시간 문자열은 UTC(`Z`) 형식으로 반환될 수 있습니다. 화면에는 `Asia/Seoul` 등 사용자의 시간대로 변환해 표시하세요.

`PetEventResponse`의 선택 필드는 `null`일 수 있습니다. 예를 들어 클립 업로드에 실패했거나 영상이 없는 이벤트는 `videoUrl`, `thumbnailUrl`, 클립 시간 필드가 모두 `null`일 수 있습니다.

### 영상 재생 처리

영상과 썸네일의 실제 파일은 PostgreSQL이 아니라 GCP Storage에 있습니다. 프론트는 API가 반환한 **브라우저에서 재생 가능한 HTTPS URL**만 `<video>` 또는 `<img>`에 사용합니다.

`gs://...` 형식은 브라우저에서 재생할 수 없습니다. Signed URL 발급 기능이 도입되기 전에는 GCS 객체 경로를 임의로 재생 URL로 가정하지 마세요.

### 화면별 구현 가능 상태

| 화면 | 현재 구현 상태 | 프론트 처리 |
| --- | --- | --- |
| 카메라 등록·목록·상세·수정 | 사용 가능 | 위 카메라 API 사용 |
| 이벤트 목록·상세 | 사용 가능 | 위 이벤트 조회 API 사용 |
| 이벤트 썸네일 | 조건부 사용 가능 | `thumbnailUrl`이 HTTPS URL일 때만 표시 |
| 이벤트 영상 재생 | 보류 | Signed HTTPS URL 발급 API가 배포된 뒤 연결 |
| 고화질 실시간 보기 | 미구현 | MediaMTX 시작·중지·재생 API 배포 전까지 화면을 비활성화 또는 준비 중으로 표시 |

## 프론트엔드에서 사용하지 않는 내부 기능

아래 기능은 장비·서버 간 통신용입니다. 프론트엔드에서 호출하지 않습니다.

```text
/api/internal/*
/api/raspberrypis/*
Raspberry Pi lastIp, MAC 주소, RTSP 주소, RTSP 계정·비밀번호
FastAPI 내부 API 키
```

## 개발 확인 방법

- 로컬 Spring 서버: `http://localhost:8080/swagger-ui/index.html`
- API 주소와 토큰은 환경별 설정으로 관리합니다.
- 서버가 반환하는 실제 요청·응답 스키마는 Swagger UI를 최종 기준으로 확인합니다.
