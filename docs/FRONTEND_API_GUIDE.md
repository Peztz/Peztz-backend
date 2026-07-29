# Peztz 프론트엔드 API 연동 가이드

## 서버 주소

Spring Boot API 기본 주소:

```text
http://34.50.7.78:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
http://34.50.7.78:8080/swagger-ui/index.html
```

기존 FastAPI 영상 프록시 주소:

```text
http://34.50.7.78:8000/video/{deviceId}
```

## 운영 DB 유의사항

Spring 도메인 API는 아래 운영 테이블을 재사용합니다.

```text
users, "Pets", hospitals, cage, access_session, pet_logs, pet_videos, raspberrypi
```

`docs/sql/peztz_domain_schema.sql`은 폐기된 경고용 파일이므로 운영 DB에 적용하면 안 됩니다.

배포 전에는 아래 마이그레이션을 적용합니다.

```text
docs/sql/peztz_domain_migration.sql
```

마이그레이션은 `users.password` 길이를 확장하고, `auth_token`을 생성하며,
`access_session.session_id`와 `pet_logs.log_id`의 시퀀스·기본값을 보완합니다.

## 권장 연동 순서

1. `POST /api/auth/signup`
2. `POST /api/auth/login`
3. `GET /api/auth/me`
4. `POST /api/pets`
5. `GET /api/pets/my`
6. `GET /api/facilities`
7. `GET /api/facilities/{facilityId}/cages`
8. `POST /api/admission-sessions`
9. `POST /api/admission-sessions/access-code/verify`
10. `GET /api/owners/me/cages`
11. `GET /api/admission-sessions/{sessionId}/logs`
12. `GET /api/reports/daily?petId={petId}&date=YYYY-MM-DD`
13. 기존 영상은 `http://34.50.7.78:8000/video/{deviceId}`로 표시

## 인증 흐름

회원가입 API입니다. `phoneNumber`는 API 호환성을 위해 받지만 현재 `users` 테이블에는 저장하지 않습니다.

```http
POST /api/auth/signup
Content-Type: application/json

{
  "email": "owner@example.com",
  "password": "password1234",
  "name": "Owner",
  "phoneNumber": "010-1234-5678",
  "role": "OWNER"
}
```

로그인 후 반환된 `accessToken`을 저장합니다.

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
    "name": "Owner",
    "role": "OWNER"
  }
}
```

인증이 필요한 API는 아래 헤더를 사용합니다.

```http
Authorization: Bearer sample-token
```

## 반려동물 흐름

반려동물은 기존 `"Pets"` 테이블에 저장합니다. `name`, `breed`, `memo`는 저장됩니다.
`species`, `gender`, `birthDate`, `weightKg`는 nullable API 필드로 받지만, DB 스키마를 확장하기 전까지는 저장하지 않습니다.

```http
POST /api/pets
Authorization: Bearer sample-token
Content-Type: application/json

{
  "name": "Choco",
  "species": "DOG",
  "breed": "Poodle",
  "gender": "MALE",
  "birthDate": "2022-03-01",
  "weightKg": 5.2,
  "memo": "Shy around strangers"
}
```

```http
GET /api/pets/my
Authorization: Bearer sample-token
```

기타 반려동물 API:

```text
GET /api/pets/{petId}
PUT /api/pets/{petId}
DELETE /api/pets/{petId}
```

## 시설 및 Cage 흐름

시설은 `hospitals` 테이블을 사용합니다. 현재 테이블에는 주소 컬럼이 없으므로 `address`는 `null`로 반환됩니다.

```http
GET /api/facilities
```

`cage` 테이블은 `hospital_id`, `name`, `cage_number`, `status`, 연결된 Raspberry Pi `device_id`를 보관합니다.
Raspberry Pi가 연결된 Cage 응답에는 `facilityId`, `name`, `cageNumber`, `videoUrl`이 포함됩니다.

```http
GET /api/facilities/{facilityId}/cages
```

```json
{
  "id": "d69fc7ff-481c-4305-b81c-551955a1ce23",
  "facilityId": "0e96bc6a-90a5-45cc-ac64-37d19254e7a2",
  "name": "A-1 Cage",
  "cageNumber": "A-1",
  "status": "AVAILABLE",
  "raspberryPiDeviceId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
  "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890",
  "createdAt": null
}
```

시설/Cage 관리 API:

```text
POST /api/facilities
POST /api/facilities/{facilityId}/cages
GET /api/cages
GET /api/cages/{cageId}
PUT /api/cages/{cageId}
DELETE /api/cages/{cageId}
```

시설 입실 관리 API:

```text
GET /api/facilities/{facilityId}/owners/pets?email={ownerEmail}
POST /api/facilities/{facilityId}/admission-sessions
```

위 API는 `FACILITY_MANAGER` 또는 `ADMIN` 같은 시설 역할의 `Authorization: Bearer {accessToken}`이 필요합니다.
시설 입실 API는 `POST /api/admission-sessions/access-code/verify`에서 사용하는 활성 세션과 접근 코드를 동일하게 생성합니다.

## 입실 세션 흐름

입실 세션은 `access_session`에 저장됩니다. `sessionId`는 UUID가 아닌 숫자형 `bigint`입니다.

```http
POST /api/admission-sessions
Authorization: Bearer sample-token
Content-Type: application/json

{
  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23"
}
```

```json
{
  "sessionId": 123,
  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23",
  "accessCode": "123456",
  "status": "ACTIVE",
  "startedAt": "2026-06-08T12:00:00",
  "endedAt": null,
  "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890"
}
```

세션을 생성하면 Cage 상태는 `OCCUPIED`가 됩니다. 세션을 종료하면 세션 상태는 `ENDED`, Cage 상태는 `AVAILABLE`이 됩니다.

```http
PATCH /api/admission-sessions/{sessionId}/end
Authorization: Bearer sample-token
```

## 접근 코드 흐름

```http
POST /api/admission-sessions/access-code/verify
Content-Type: application/json

{
  "accessCode": "123456"
}
```

```json
{
  "valid": true,
  "sessionId": 123,
  "petName": "Choco",
  "cageName": "cage d69fc7ff-481c-4305-b81c-551955a1ce23",
  "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890"
}
```

`ACTIVE` 상태의 세션만 접근 코드 검증에 성공합니다.

## 보호자 Cage 조회 흐름

```http
GET /api/owners/me/cages
Authorization: Bearer sample-token
```

```json
[
  {
    "sessionId": 123,
    "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
    "petName": "Choco",
    "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23",
    "cageName": "cage d69fc7ff-481c-4305-b81c-551955a1ce23",
    "facilityName": null,
    "status": "OCCUPIED",
    "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890"
  }
]
```

## 기존 영상 표시 흐름

Cage/세션 응답의 `videoUrl`을 우선 사용합니다.

```html
<img src="http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890" alt="실시간 스트림" />
```

`raspberryPiDeviceId`만 있다면 다음처럼 주소를 만들 수 있습니다.

```javascript
const videoUrl = `http://34.50.7.78:8000/video/${raspberryPiDeviceId}`;
```

기존 Raspberry Pi 메타데이터 API도 사용할 수 있습니다.

```text
GET /api/raspberrypis
GET /api/raspberrypis/{deviceId}/stream-url
GET /api/raspberrypis/stream-url?macAddress=...
```

프론트엔드의 기존 영상 재생은 FastAPI 프록시 URL을 사용합니다.

```text
http://34.50.7.78:8000/video/{deviceId}
```

`GET /api/raspberrypis/{deviceId}/stream-url`은 FastAPI가 사용하는 내부 업스트림 URL을 확인하는 용도입니다.
`raspberrypi.lastIp`는 GCP FastAPI 서버가 `http://{lastIp}:8001/video_feed`에 접근할 수 있는 Raspberry Pi Tailscale IP여야 하며, 일반적으로 `100.x.x.x`입니다.
Pi 전원이 꺼져 있거나 `camera_stream.py`가 실행 중이 아니면 `GET /video/{deviceId}`는 `502` 또는 timeout을 반환할 수 있습니다.

## 세션 로그 흐름

로그는 `pet_logs`에 저장됩니다. `type`은 `log_type`에 저장되며, `message`, `temperature`, `humidity`는 `data jsonb` 안에 저장됩니다.

```http
POST /api/admission-sessions/{sessionId}/logs
Authorization: Bearer sample-token
Content-Type: application/json

{
  "type": "SENSOR",
  "message": "sensor data received",
  "temperature": 25.4,
  "humidity": 61.2
}
```

```http
GET /api/admission-sessions/{sessionId}/logs
Authorization: Bearer sample-token
```

```json
[
  {
    "id": 456,
    "type": "SENSOR",
    "message": "sensor data received",
    "temperature": 25.4,
    "humidity": 61.2,
    "createdAt": "2026-06-08T12:30:00"
  }
]
```

## 일일 리포트 흐름

일일 리포트는 `pet_logs.data`에서 `temperature`, `humidity`를 읽습니다.

```http
GET /api/reports/daily?petId=7bf2b0d2-dd67-4002-929a-d4505f6af890&date=2026-06-08
Authorization: Bearer sample-token
```

```http
GET /api/admission-sessions/{sessionId}/daily-report?date=2026-06-08
Authorization: Bearer sample-token
```

```json
{
  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
  "date": "2026-06-08",
  "totalLogCount": 12,
  "sensorLogCount": 8,
  "averageTemperature": 25.1,
  "averageHumidity": 60.4,
  "summary": "Today has 12 logs."
}
```

해당 날짜에 로그가 없으면 `count`는 `0`, 평균값은 `null`이며 500 오류 대신 요약값을 반환합니다.

## 카메라 및 이상행동 이벤트 흐름

카메라 API는 기존 Bearer 토큰을 사용하며, 카메라가 연결된 Cage의 현재 사용자만 조회할 수 있습니다.
Cage당 카메라는 한 대만 등록할 수 있습니다. RTSP URL·계정·비밀번호·`rtspConfigKey`는 응답 DTO에 포함하지 않습니다.

```text
POST /api/cameras
GET /api/cameras/my
GET /api/cameras/{cameraId}
PUT /api/cameras/{cameraId}
GET /api/cameras/{cameraId}/runtime-status
```

`rtspConfigKey`는 Raspberry Pi 또는 시크릿 저장소에 보관한 RTSP 설정의 선택적 참조값입니다. RTSP URL 자체가 아닙니다.

카메라 등록 요청에는 `cageId`, `name`, 선택적으로 `rtspConfigKey`를 전달합니다.
카메라 수정은 `name`, `rtspConfigKey`만 받으며, 다른 Cage로 카메라를 옮기는 기능은 의도적으로 지원하지 않습니다.

FastAPI는 내부 API 키로 보호된 아래 API에 이상행동 메타데이터를 전달합니다.

```text
POST /api/internal/pet-events
X-Internal-Api-Key: {PEZTZ_INTERNAL_API_KEY}
```

요청 본문에는 `externalEventId`, `petId`, `cameraId`, `eventType`, `confidence`, `occurredAt`, `videoUrl`, `thumbnailUrl`, 선택적 `metadata`가 포함됩니다.
행동이 지속된 시간과 영상 클립 구간도 함께 기록하려면 선택적으로 `eventEndedAt`, `eventDurationSeconds`, `clipStartAt`, `clipEndAt`, `clipDurationSeconds`를 보냅니다.
같은 `externalEventId`를 다시 보내면 기존 이벤트를 반환합니다. Spring은 수신 시점에 해당 반려동물이 카메라 Cage의 현재 반려동물인지 검증합니다.

보호자는 아래 API로 이벤트를 조회합니다.

```text
GET /api/pet-events/my
GET /api/pet-events/my?petId={petId}
GET /api/pet-events/{eventId}
```

AI 이벤트는 기존 `pet_logs`의 `AI_EVENT` 행으로 저장됩니다. FastAPI가 보낸 `occurredAt`은 `pet_logs.created_at`에 저장하며, 이는 **이상행동이 시작되거나 감지된 시각**입니다. `eventEndedAt`과 `eventDurationSeconds`는 행동 자체가 종료된 시각과 지속 시간을 저장합니다.
이벤트 클립 URL과 썸네일 URL은 로그가 참조하는 기존 `pet_videos` 행에 저장합니다. `clipStartAt`, `clipEndAt`, `clipDurationSeconds`는 전후 버퍼를 포함한 **증거 영상 클립**의 구간입니다. 시작·종료 시각이 모두 전달되면 Spring이 그 차이로 지속 시간을 계산해 저장하며, 종료 시각 없이 시작 시각과 지속 시간만 전달하면 종료 시각을 계산합니다.
예를 들어 행동은 `14:03:10`부터 `14:03:15`까지 5초였지만, 확인 영상은 전후 버퍼를 포함해 `14:03:00`부터 `14:03:25`까지 25초일 수 있습니다. 영상·이미지 바이너리는 PostgreSQL에 저장하지 않습니다.
응답의 시간 값은 UTC(`Z`) 형식일 수 있으므로, 프론트엔드는 사용자 시간대(한국은 `Asia/Seoul`)로 변환해 표시합니다.
