# Peztz Backend

Tapo C225 IP 카메라와 Raspberry Pi를 이용하는 반려동물 모니터링 백엔드입니다.

## 구성

- `spring-server`: 인증, 반려동물·케이지·카메라·이상행동 이벤트 메타데이터를 관리하는 Spring Boot 서버
- `fastapi-stream-server`: GCP MediaMTX 상태 조회와 Raspberry Pi 이벤트 전달을 담당하는 내부 연동 서버
- `raspberry-pi`: Raspberry Pi 장치 등록 및 Tapo RTSP 자동 송출 서비스
- `infra/mediamtx`: GCP MediaMTX Docker Compose와 테스트 설정
- `docs`: 프론트엔드 API 가이드와 PostgreSQL 마이그레이션 문서

## 영상 처리 구조

```text
Tapo C225 stream2 (저화질 RTSP)
  -> Raspberry Pi FFmpeg systemd 서비스
  -> GCP MediaMTX cage-a1
  -> 보호자 실시간 보기

GCP FastAPI
  -> MediaMTX Control API에서 cage-a1 온라인 상태 조회
  -> Spring Boot에 상태와 재생 URL 반환

이상행동 발생 시 (추후 구현)
  -> Raspberry Pi / FastAPI가 클립·썸네일 생성 및 GCS 업로드
  -> videoUrl, thumbnailUrl만 Spring Boot로 전달
```

Raspberry Pi Camera Module 기반 MJPEG 스트림과 `/video/*` 프록시 API는 사용하지 않습니다.

## 현재 구현 범위

- 케이지당 카메라 1대 등록 및 조회
- FastAPI의 이상행동 이벤트 전달 API
- `pet_logs`와 `pet_videos`를 이용한 이벤트·클립 메타데이터 저장
- Raspberry Pi 부팅 및 장애 복구 시 FFmpeg 자동 송출
- FastAPI에서 MediaMTX 실제 송출 상태와 재생 URL 조회
- Spring에서 FastAPI 카메라 상태를 조회하는 목업/HTTP 어댑터

실제 RTSP 수신과 MediaMTX 송출은 Raspberry Pi 서비스가 담당합니다. YOLO 추론과 GCS 이벤트 클립 업로드는 다음 단계에서 구현합니다.

## 실행 전 준비

Spring Boot 실행 전 운영 PostgreSQL에 [마이그레이션 SQL](docs/sql/peztz_domain_migration.sql)을 적용해야 합니다.

SmartThings 케이지 센서 설정과 API 호출 순서는
[SmartThings 센서 연동 문서](docs/SMARTTHINGS_SENSOR_INTEGRATION.md)를 참고하세요.

환경변수로 DB 연결 정보와 내부 API 키를 설정하세요. RTSP 주소·계정·비밀번호는 Spring 응답이나 저장소에 두지 않습니다.

프론트엔드 연동 내용은 [FRONTEND_API_GUIDE.md](docs/FRONTEND_API_GUIDE.md)를 참고하세요.
