# GCP MediaMTX 배포 설정

이 설정은 라즈베리파이가 전송한 영상을 GCP에서 실시간으로 중계합니다.
GCP VM에는 전체 영상을 계속 녹화하지 않으며, Control API는 호스트의
로컬 주소에만 연결하여 같은 서버의 FastAPI가 `cage-a1` 상태를 확인합니다.

## 설정파일 준비

```bash
cd infra/mediamtx
cp mediamtx.yml.example mediamtx.yml
nano mediamtx.yml
```

`mediamtx.yml`에서 GCP 공인 IP 또는 스트리밍 도메인과 송출 계정 정보를
실제 값으로 변경합니다. 경로와 송출 계정은 라즈베리파이의
`/etc/peztz/camera-stream.env` 설정과 같아야 합니다.

`mediamtx.yml`에는 계정 정보가 들어가므로 Git에 커밋하지 않습니다.
이 파일은 `.gitignore`에 등록되어 있습니다.

## 실행

기존 `mediamtx` 컨테이너를 `docker run`으로 만들었다면 Control API 포트
9997을 추가하기 위해 최초 한 번만 기존 컨테이너를 교체합니다.

```bash
docker stop mediamtx
docker rm mediamtx
docker compose up -d
```

이후에는 다음 명령으로 실행 상태와 로그를 확인합니다.

```bash
docker compose up -d
docker compose ps
docker compose logs -f mediamtx
```

## Control API 확인

GCP 서버 안에서만 다음 주소에 접근할 수 있습니다.

```bash
curl http://127.0.0.1:9997/v3/paths/list
curl http://127.0.0.1:9997/v3/paths/get/cage-a1
```

라즈베리파이가 송출 중이면 `ready`와 `online`이 `true`로 표시됩니다.
송출 전에는 경로가 존재해도 두 값이 `false`일 수 있습니다.

## 녹화 정책

`record: false`는 의도된 설정입니다. 전체 영상은 IP 카메라의 microSD에
순환 녹화하고, MediaMTX는 실시간 중계만 담당합니다. 이상행동 전후 클립
생성과 GCS 업로드는 별도 기능으로 구현합니다.

## GCP 서비스 환경변수

GCP FastAPI 서비스에 다음 값을 설정합니다.

```text
MEDIAMTX_API_BASE_URL=http://127.0.0.1:9997
MEDIAMTX_PLAYBACK_BASE_URL=http://GCP_공인_IP:8889
MEDIAMTX_STREAM_PATH=cage-a1
```

같은 VM의 Spring Boot 서비스에는 다음 값을 설정합니다.

```text
FASTAPI_CLIENT_MODE=http
FASTAPI_BASE_URL=http://127.0.0.1:8000
FASTAPI_INTERNAL_API_KEY=FastAPI와_같은_내부_API_키
```

예제 설정은 연동 테스트를 위해 익명 영상 재생을 허용합니다. 실제 서비스에
공개하기 전에는 시청자 인증과 HTTPS를 반드시 적용해야 합니다.
