# MediaMTX 설정

MediaMTX는 Spring Boot, FastAPI와 함께 상위 [Docker Compose](../README.md)에서 실행합니다.
이 폴더의 `mediamtx.yml.example`은 운영 설정을 만들기 위한 예시 파일입니다.

## 운영 설정 준비

저장소의 `infra` 폴더에서 실행합니다.

```bash
cp mediamtx/mediamtx.yml.example mediamtx/mediamtx.yml
nano mediamtx/mediamtx.yml
```

다음 값을 실제 운영 정보로 변경합니다.

- `GCP_PUBLIC_IP_OR_STREAM_DOMAIN`: GCP 공인 IP 또는 스트리밍 도메인
- `CHANGE_ME_PUBLISH_USER`: Raspberry Pi 송출 계정
- `CHANGE_ME_PUBLISH_PASSWORD`: Raspberry Pi 송출 비밀번호
- `cage-a1`: Raspberry Pi가 송출하는 MediaMTX 경로

실제 `mediamtx.yml`에는 인증 정보가 포함되므로 Git에 커밋하지 않습니다.

## 현재 정책

- RTSP 송출은 TCP `8554` 포트를 사용합니다.
- WebRTC는 TCP `8889`와 UDP `8189`를 사용합니다.
- Control API `9997`은 호스트 루프백과 Compose 내부 네트워크에서만 접근합니다.
- `record: false`이므로 GCP 서버에 전체 영상을 녹화하지 않습니다.
- 익명 재생은 연동 테스트용이며 운영 공개 전에 시청 인증과 HTTPS를 적용해야 합니다.

실행 및 로그 확인 방법은 상위 [인프라 README](../README.md)를 참고하세요.
