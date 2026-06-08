# Peztz Frontend API Guide

Spring Boot API Base URL:

```text
http://34.50.7.78:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
http://34.50.7.78:8080/swagger-ui/index.html
```

FastAPI video proxy:

```text
http://34.50.7.78:8000/video/{deviceId}
```

## Production DB Notes

The Spring domain API reuses the existing production tables:

```text
users, "Pets", hospitals, cage, access_session, pet_logs, pet_videos, raspberrypi
```

Do not apply `docs/sql/peztz_domain_schema.sql` to production. It is deprecated and kept only as a warning file.

Apply this migration before deployment:

```text
docs/sql/peztz_domain_migration.sql
```

The migration expands `users.password`, creates `auth_token`, and adds sequence/default settings for `access_session.session_id` and `pet_logs.log_id`.

## Recommended Integration Order

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
13. Render video with `http://34.50.7.78:8000/video/{deviceId}`

## Auth Flow

Create an account. `phoneNumber` is accepted for API compatibility but the current `users` table does not store it.

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

Login and store `accessToken`:

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

Use protected APIs with:

```http
Authorization: Bearer sample-token
```

## Pet Flow

Pets are stored in the existing `"Pets"` table. `name`, `breed`, and `memo` are persisted. `species`, `gender`, `birthDate`, and `weightKg` are accepted as nullable API fields but are not persisted unless the DB schema is extended later.

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

Other pet APIs:

```text
GET /api/pets/{petId}
PUT /api/pets/{petId}
DELETE /api/pets/{petId}
```

## Facility And Cage Flow

Facilities are backed by `hospitals`. `address` is returned as `null` because the current table has no address column.

```http
GET /api/facilities
```

The existing `cage` table has no direct `hospital_id`, `name`, or `cage_number`. Therefore cage responses use calculated/null fields where needed.

```http
GET /api/facilities/{facilityId}/cages
```

```json
{
  "id": "d69fc7ff-481c-4305-b81c-551955a1ce23",
  "facilityId": null,
  "name": "cage d69fc7ff-481c-4305-b81c-551955a1ce23",
  "cageNumber": null,
  "status": "AVAILABLE",
  "raspberryPiDeviceId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
  "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890",
  "createdAt": null
}
```

Management APIs:

```text
POST /api/facilities
POST /api/facilities/{facilityId}/cages
GET /api/cages
GET /api/cages/{cageId}
PUT /api/cages/{cageId}
DELETE /api/cages/{cageId}
```

## Admission Session Flow

Admission sessions are stored in `access_session`. `sessionId` is a numeric `bigint`, not a UUID.

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

Creating a session sets cage status to `OCCUPIED`. Ending a session sets session status to `ENDED` and cage status to `AVAILABLE`.

```http
PATCH /api/admission-sessions/{sessionId}/end
Authorization: Bearer sample-token
```

## Access Code Flow

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

Only `ACTIVE` sessions verify successfully.

## Owner Cage Flow

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

## Video Display Flow

Prefer the `videoUrl` from cage/session responses:

```html
<img src="http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890" alt="live stream" />
```

If only `raspberryPiDeviceId` is available:

```javascript
const videoUrl = `http://34.50.7.78:8000/video/${raspberryPiDeviceId}`;
```

Existing Raspberry Pi metadata APIs remain available:

```text
GET /api/raspberrypis
GET /api/raspberrypis/{deviceId}/stream-url
GET /api/raspberrypis/stream-url?macAddress=...
```

## Session Log Flow

Logs are stored in `pet_logs`. `type` maps to `log_type`; `message`, `temperature`, and `humidity` are stored inside `data jsonb`.

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

## Daily Report Flow

Daily reports read `temperature` and `humidity` from `pet_logs.data`.

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

If there are no logs for the date, the API returns count `0`, null averages, and a summary instead of a 500 error.
