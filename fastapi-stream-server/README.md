# Peztz FastAPI 장치 연동 서버

이 서비스는 GCP에서 Raspberry Pi 비전 처리 결과를 Spring Boot에 전달하고 MediaMTX 송출 상태를 조회합니다.

Raspberry Pi Camera Module 기반 MJPEG 프록시와 `/video/*` API는 사용하지 않습니다.

## 현재 API

- `GET /`, `GET /health`
- `POST /register`: Raspberry Pi 장치 등록을 Spring 내부 API로 전달
- `POST /device/{cage_id}/sensor`: 기존 센서 연동 호환 API
- `POST /device/events`: YOLO/비전 처리 결과를 Spring의 이상행동 이벤트 API로 전달
- `GET /internal/cameras/{camera_id}/status`
- `POST /internal/cameras/{camera_id}/live/start`
- `POST /internal/cameras/{camera_id}/live/stop`

카메라 상태 API는 MediaMTX Control API에서 실제 송출 상태를 조회합니다. FFmpeg는 라즈베리파이의 `peztz-camera` systemd 서비스가 관리하므로 GCP의 live start/stop API는 프로세스를 직접 시작하거나 종료하지 않습니다.

## 환경변수

```text
SPRING_BOOT_BASE_URL            # 예: https://spring.example.com
SPRING_INTERNAL_API_KEY         # FastAPI -> Spring 내부 호출 키
DEVICE_API_KEY                  # Raspberry Pi -> FastAPI 호출 키
FASTAPI_INTERNAL_API_KEY        # Spring -> FastAPI 내부 호출 키
MEDIAMTX_API_BASE_URL           # 기본값: http://127.0.0.1:9997
MEDIAMTX_PLAYBACK_BASE_URL      # 예: http://34.50.7.78:8889
MEDIAMTX_STREAM_PATH            # 단일 카메라 테스트 경로, 예: cage-a1
MEDIAMTX_STREAM_PATH_TEMPLATE   # 경로 미지정 시 기본값: camera-{camera_id}
```

값은 소스 코드에 기본값으로 넣지 말고 배포 환경에서 설정합니다.

## 설치 및 실행

```bash
cd fastapi-stream-server
python -m venv .venv
pip install -r requirements-dev.txt
python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

## 테스트

```bash
cd fastapi-stream-server
pytest
```

GCP 한 대 테스트에서는 `MEDIAMTX_STREAM_PATH=cage-a1`을 설정합니다. 여러 카메라로 확장할 때는 라즈베리파이 송출 경로를 `camera-{cameraId}` 형식으로 맞추고 고정 경로 환경변수를 제거합니다.

실제 RTSP 수신은 라즈베리파이가 담당합니다. YOLO와 GCS 업로드 기능이 추가되면 해당 기능별 통합 테스트를 추가해야 합니다.
