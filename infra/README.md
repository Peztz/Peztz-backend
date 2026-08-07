# Peztz Docker Compose

이 Compose는 GCP VM에서 Spring Boot, FastAPI, MediaMTX를 함께 관리합니다.
PostgreSQL은 기존 GCP 호스트 설치를 그대로 사용하며 Compose에 포함하지 않습니다.

## 운영 상태

- 2026-08-07 기준 Spring Boot와 FastAPI 운영 서비스를 Docker Compose로 전환했습니다.
- `main` 브랜치의 Spring, FastAPI 또는 `infra` 변경은 Docker 운영 자동배포로 반영됩니다.
- 기존 systemd 서비스는 비상복구용으로 비활성화된 상태로 유지합니다.
- 운영 롤백은 직전 Docker 이미지 복구를 우선으로 사용합니다.

## 파일 준비

저장소 루트에서 다음 파일을 준비합니다.

```bash
cd infra
cp .env.example .env
cp mediamtx/mediamtx.yml.example mediamtx/mediamtx.yml
```

`.env`에는 DB 비밀번호와 내부 API 키를 입력합니다. `mediamtx.yml`에는 GCP 공인 IP,
송출 계정, 송출 비밀번호를 입력합니다. 두 파일은 Git에 커밋하지 않습니다.

GCP 운영 서버의 `infra/.env`는 다음 주소와 포트를 사용합니다.

```text
SPRING_BIND_ADDRESS=0.0.0.0
SPRING_HOST_PORT=8080
FASTAPI_BIND_ADDRESS=0.0.0.0
FASTAPI_HOST_PORT=8000
```

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

## GitHub Actions 배포

### 운영 자동배포

GitHub 저장소의 `Settings > Secrets and variables > Actions > Variables`에는 다음
Repository variable이 설정돼 있어야 합니다.

```text
PEZTZ_DOCKER_PRODUCTION_ENABLED=true
```

이 값이 `true`이면 `main`의 Spring, FastAPI 또는 `infra` 변경 시
`Deploy Docker Production to GCP`가 자동 실행됩니다. 수동 실행도 지원하지만 운영
자동배포 검증은 `main`의 관련 경로 변경으로 확인합니다.

같은 경로를 변경한 Pull Request에서는 `validate` 작업만 실행하여 Spring과 FastAPI
테스트를 먼저 확인합니다. PR 검증에서는 GCP에 접속하거나 운영 컨테이너를 교체하지
않습니다. `main` 반영 후에는 검증이 통과한 경우에만 `deploy` 작업이 이어집니다.

배포 전에 다음 조건을 충족해야 합니다.

- GCP 배포 저장소에 추적 중인 로컬 변경사항이 없어야 합니다.
- `infra/.env`가 `0.0.0.0:8080`, `0.0.0.0:8000`으로 설정돼 있어야 합니다.
- 기존 `mediamtx` 컨테이너가 실행 중이어야 합니다.
- 배포 사용자가 두 systemd 서비스를 비밀번호 없이 시작·중지·활성화·비활성화할 수 있어야 합니다.

워크플로는 Spring과 FastAPI 테스트 및 이미지 빌드를 완료한 뒤 운영 컨테이너를
교체합니다. 컨테이너 상태, 공개 API, 컨테이너 간 통신과 MediaMTX 상태를 검사하며,
실패하면 직전 Docker 이미지 또는 비상용 systemd 서비스로 자동 복구합니다. 성공한
배포의 커밋과 이미지 정보는 서버의 `~/.peztz-deploy`에 기록합니다.

Spring 컨테이너와 Docker 배포 상태 확인에는 `/actuator/health`를 사용합니다. 이전
Actuator 도입 전 Docker 이미지와 systemd 서비스로 복구할 때만 `/v3/api-docs`를
호환 확인 경로로 사용합니다.

### 운영 롤백

`Roll Back Docker Production on GCP` 수동 액션은 `ROLLBACK` 확인 문자열과 함께 다음
대상 중 하나를 선택합니다.

- `previous_docker`: 직전에 사용하던 Spring/FastAPI 이미지로 복구
- `systemd`: Docker 애플리케이션 컨테이너를 제거하고 기존 systemd 서비스로 복구

`systemd`로 복구할 때는 먼저 `PEZTZ_DOCKER_PRODUCTION_ENABLED`를 `false`로 바꿔서
다음 `main` 변경이 다시 Docker 운영 배포를 실행하지 않게 합니다. systemd 서비스는
Docker 애플리케이션 컨테이너와 같은 포트를 사용하므로 동시에 실행하지 않습니다.

## 연결 주소

Compose 내부에서는 컨테이너 서비스 이름을 사용합니다.

```text
Spring -> FastAPI: http://fastapi:8000
FastAPI -> Spring: http://spring:8080
FastAPI -> MediaMTX: http://mediamtx:9997
Spring/FastAPI -> 호스트 PostgreSQL: host.docker.internal:5432
```

## 주의사항

- PostgreSQL이 Docker 브리지 네트워크의 접속을 허용해야 합니다.
- 운영 배포는 기존 `mediamtx` 컨테이너를 중지하거나 재생성하지 않습니다.
- 기존 `peztz-spring`, `peztz-api` systemd 서비스는 비활성화 상태로 유지합니다.
- systemd 비상복구를 실행하기 전에는 `PEZTZ_DOCKER_PRODUCTION_ENABLED=false`로 변경합니다.
- `docker compose down -v`는 사용하지 않습니다.
