# Peztz Docker Compose

이 Compose는 GCP VM에서 Spring Boot, FastAPI, MediaMTX를 함께 관리합니다.
PostgreSQL은 기존 GCP 호스트 설치를 그대로 사용하며 Compose에 포함하지 않습니다.

## 파일 준비

저장소 루트에서 다음 파일을 준비합니다.

```bash
cd infra
cp .env.example .env
cp mediamtx/mediamtx.yml.example mediamtx/mediamtx.yml
```

`.env`에는 DB 비밀번호와 내부 API 키를 입력합니다. `mediamtx.yml`에는 GCP 공인 IP,
송출 계정, 송출 비밀번호를 입력합니다. 두 파일은 Git에 커밋하지 않습니다.

기존 systemd 서비스와 병행 테스트할 때는 다음 포트를 유지합니다.

```text
SPRING_HOST_PORT=18080
FASTAPI_HOST_PORT=18000
```

운영 전환 시 기존 서비스를 중지한 다음 각각 `8080`, `8000`으로 변경합니다.

## 설정 확인과 빌드

```bash
docker compose config
docker compose build
```

## 실행

```bash
docker compose up -d
docker compose ps
docker compose logs -f
```

개별 로그는 서비스 이름으로 확인합니다.

```bash
docker compose logs -f spring
docker compose logs -f fastapi
docker compose logs -f mediamtx
```

## 연결 주소

Compose 내부에서는 컨테이너 서비스 이름을 사용합니다.

```text
Spring -> FastAPI: http://fastapi:8000
FastAPI -> Spring: http://spring:8080
FastAPI -> MediaMTX: http://mediamtx:9997
Spring/FastAPI -> 호스트 PostgreSQL: host.docker.internal:5432
```

## 주의사항

- 운영 전환 전 PostgreSQL이 Docker 브리지 네트워크의 접속을 허용하는지 확인해야 합니다.
- 기존 `mediamtx` 컨테이너가 실행 중이면 새 Compose의 MediaMTX와 이름 및 포트가 충돌합니다.
- 운영 전환 전까지 기존 systemd 서비스와 MediaMTX 컨테이너를 중지하지 않습니다.
- `docker compose down -v`는 사용하지 않습니다.
