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
SPRING_BIND_ADDRESS=127.0.0.1
SPRING_HOST_PORT=18080
FASTAPI_BIND_ADDRESS=127.0.0.1
FASTAPI_HOST_PORT=18000
```

운영 전환 전에는 GCP 서버의 `infra/.env`를 다음처럼 변경합니다. 운영 배포
워크플로가 값을 확인한 다음 기존 systemd 서비스를 중지하므로 직접 중지하지 않습니다.

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

### Docker 병렬 검증

`Validate Docker Compose on GCP`는 기존 systemd 운영 서비스를 유지한 채 Spring과
FastAPI를 `127.0.0.1:18080`, `127.0.0.1:18000`에 배포하는 수동 검증 액션입니다.
운영 트래픽을 Docker로 전환하지 않습니다.

### 최초 운영 전환

`Deploy Docker Production to GCP`는 처음에는 GitHub Actions에서 수동으로 실행합니다.
다음 조건을 전환 전에 충족해야 합니다.

- GCP 배포 저장소에 추적 중인 로컬 변경사항이 없어야 합니다.
- `infra/.env`가 `0.0.0.0:8080`, `0.0.0.0:8000`으로 설정돼 있어야 합니다.
- 기존 `peztz-spring`, `peztz-api` systemd 서비스가 모두 실행 중이어야 합니다.
- 기존 `mediamtx` 컨테이너가 실행 중이어야 합니다.
- 배포 사용자가 두 systemd 서비스를 비밀번호 없이 시작·중지·활성화·비활성화할 수 있어야 합니다.

워크플로는 Spring과 FastAPI 테스트 및 이미지 빌드를 먼저 완료하고 마지막에 systemd
서비스를 중지합니다. 컨테이너·API·서비스 간 통신 검사가 실패하면 기존 systemd
서비스 또는 직전 Docker 이미지로 자동 복구합니다. 최초 전환 성공 시에는 서버 재부팅
후 포트 충돌이 발생하지 않도록 기존 systemd 서비스도 비활성화합니다.

수동 운영 전환 성공 후 공개 API와 장비 연동을 확인한 다음 GitHub 저장소의
`Settings > Secrets and variables > Actions > Variables`에서 다음 Repository variable을
추가합니다.

```text
PEZTZ_DOCKER_PRODUCTION_ENABLED=true
```

이 값이 `true`이면 이후 `main`의 Spring, FastAPI 또는 `infra` 변경 시 운영 Docker
배포가 자동 실행되고 기존 Spring/FastAPI systemd 배포 작업은 건너뜁니다. 최초 수동
전환이 성공하기 전에는 이 값을 만들지 않거나 `false`로 유지합니다.

### 운영 롤백

`Roll Back Docker Production on GCP` 수동 액션은 `ROLLBACK` 확인 문자열과 함께 다음
대상 중 하나를 선택합니다.

- `previous_docker`: 직전에 사용하던 Spring/FastAPI 이미지로 복구
- `systemd`: Docker 애플리케이션 컨테이너를 제거하고 기존 systemd 서비스로 복구

`systemd`로 복구할 때는 먼저 `PEZTZ_DOCKER_PRODUCTION_ENABLED`를 `false`로 바꿔서
다음 `main` 변경이 다시 Docker 운영 배포를 실행하지 않게 합니다. 병렬 Docker 검증을
다시 사용할 경우에는 `infra/.env`의 주소와 포트도 `127.0.0.1:18080`,
`127.0.0.1:18000`으로 되돌립니다.

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
- 최초 운영 전환 후 자동배포 활성화 전까지는 애플리케이션 변경을 `main`에 머지하지 않습니다.
- `docker compose down -v`는 사용하지 않습니다.
