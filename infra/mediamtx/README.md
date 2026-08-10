# MediaMTX 독립 Compose

MediaMTX는 Spring Boot와 FastAPI의 자동배포와 분리하여 이 Compose로 관리합니다. 앱
배포는 실행 중인 `mediamtx` 컨테이너가 그대로 유지되는지만 검사하고 중지하거나
재생성하지 않습니다. 공용 `peztz-internal` 네트워크는 상위 애플리케이션 Compose가
생성하며 MediaMTX Compose는 기존 네트워크에 연결만 합니다.

## 파일 역할

- `docker-compose.yml`: MediaMTX 컨테이너, 포트, 마운트와 공용 네트워크 정의
- `.env.example`: 운영 서버의 이미지와 기존 데이터 경로 예시
- `mediamtx.yml.example`: 새 환경에서 사용할 설정 예시

실제 운영 설정에는 송출 계정과 비밀번호가 포함되므로 Git에 커밋하지 않습니다. 현재
GCP 운영 설정과 녹화 경로는 다음 위치를 그대로 사용합니다.

```text
/home/junghyun47483/mediamtx/mediamtx.yml
/home/junghyun47483/mediamtx/recordings
```

## 설정 준비

GCP 저장소에서 다음을 실행합니다.

```bash
cd ~/Peztz-backend-docker/infra/mediamtx
cp .env.example .env
nano .env
docker compose --env-file .env config
```

최초 전환 전에 `.env`의 `MEDIAMTX_IMAGE`가 현재 실행 중인 컨테이너 이미지와 같은지,
두 호스트 경로가 실제 운영 파일과 디렉터리를 가리키는지 반드시 확인합니다.

```bash
docker inspect --format '{{.Config.Image}}' mediamtx
docker inspect --format '{{range .Mounts}}{{println .Source "->" .Destination}}{{end}}' mediamtx
docker network inspect peztz-internal >/dev/null
```

## 기존 컨테이너의 최초 전환

실제 스트리밍이 없는 시간에 진행합니다. 기존 컨테이너와 설정을 바로 삭제하지 않고
정지된 복구본으로 보관합니다.

1. `docker inspect mediamtx` 결과와 `mediamtx.yml`을 별도 백업합니다.
2. 기존 `mediamtx` 컨테이너를 중지합니다.
3. 기존 컨테이너 이름을 날짜가 포함된 `mediamtx-legacy-*`로 변경합니다.
4. 이 Compose로 새 `mediamtx` 컨테이너를 실행합니다.
5. Control API, WebRTC 재생과 FastAPI 카메라 상태 API를 확인합니다.
6. 안정화 기간이 끝날 때까지 기존 컨테이너를 삭제하지 않습니다.

컨테이너 실행과 상태 확인은 다음 명령을 사용합니다.

```bash
docker compose --env-file .env up -d
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100 mediamtx
curl --fail --silent http://127.0.0.1:9997/v3/paths/list
```

전환 중에는 짧은 스트리밍 중단이 발생할 수 있습니다. 앱 자동배포와 동시에 진행하지
않습니다.

## 전환 실패 시 복구

새 컨테이너 검증에 실패하면 새 Compose 컨테이너만 내린 뒤 보관한 기존 컨테이너의
이름을 `mediamtx`로 되돌리고 다시 시작합니다. 바인드 마운트된 설정과 녹화 데이터는
삭제하지 않습니다.

```bash
docker compose --env-file .env down
docker rename mediamtx-legacy-YYYYMMDDHHMMSS mediamtx
docker start mediamtx
```

`docker compose down -v`와 운영 설정·녹화 경로 삭제 명령은 사용하지 않습니다.

## 현재 포트와 정책

- RTSP 송출은 TCP `8554` 포트를 사용합니다.
- HLS는 TCP `8888` 포트를 사용합니다.
- WebRTC는 TCP `8889`와 UDP `8189`를 사용합니다.
- Control API `9997`은 호스트 루프백과 `peztz-internal` 네트워크에서만 접근합니다.
- 예시 설정은 `record: false`이며 실제 운영 설정은 서버 파일을 기준으로 합니다.
- 익명 재생은 연동 테스트용이며 운영 공개 전 시청 인증과 HTTPS 적용을 검토합니다.
