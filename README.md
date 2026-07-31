# Peztz Backend

Tapo C225 IP 카메라와 Raspberry Pi를 이용하는 반려동물 모니터링 백엔드입니다.

## 구성

- `spring-server`: 인증, 반려동물·케이지·카메라·이상행동 이벤트 메타데이터를 관리하는 Spring Boot 서버
- `fastapi-stream-server`: Raspberry Pi/FastAPI와 Spring 사이의 내부 연동 및 비전 처리 확장 지점
- `raspberry-pi`: Raspberry Pi 장치 등록 코드
- `docs`: 프론트엔드 API 가이드와 PostgreSQL 마이그레이션 문서

## 영상 처리 구조

```text
Tapo C225 stream2 (저화질 RTSP)
  -> Raspberry Pi / FastAPI
  -> YOLO 행동 분석
  -> Spring Boot 내부 이벤트 API
  -> PostgreSQL (이벤트 메타데이터, 영상 URL)

이상행동 발생 시
  -> Raspberry Pi / FastAPI가 클립·썸네일 생성 및 GCS 업로드
  -> videoUrl, thumbnailUrl만 Spring Boot로 전달

사용자 실시간 보기 요청 시 (다음 단계)
  -> Spring Boot 권한 확인
  -> FastAPI/Pi에 송출 시작 요청
  -> Tapo C225 stream1 (고화질 RTSP) -> MediaMTX
```

Raspberry Pi Camera Module 기반 MJPEG 스트림과 `/video/*` 프록시 API는 사용하지 않습니다.

## 현재 구현 범위

- 케이지당 카메라 1대 등록 및 조회
- FastAPI의 이상행동 이벤트 전달 API
- `pet_logs`와 `pet_videos`를 이용한 이벤트·클립 메타데이터 저장
- Spring에서 FastAPI 카메라 상태를 조회하는 목업/HTTP 어댑터

실제 RTSP 수신, YOLO 추론, GCS 업로드, MediaMTX 송출은 FastAPI/Raspberry Pi 비전 처리 단계에서 구현합니다.

## 실행 전 준비

Spring Boot 실행 전 운영 PostgreSQL에 [마이그레이션 SQL](docs/sql/peztz_domain_migration.sql)을 적용해야 합니다.

환경변수로 DB 연결 정보와 내부 API 키를 설정하세요. RTSP 주소·계정·비밀번호는 Spring 응답이나 저장소에 두지 않습니다.

프론트엔드 연동 내용은 [FRONTEND_API_GUIDE.md](docs/FRONTEND_API_GUIDE.md)를 참고하세요.
