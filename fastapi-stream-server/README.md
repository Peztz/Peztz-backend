# Peztz FastAPI 장치 연동 서버

이 서비스는 Raspberry Pi/FastAPI 비전 처리 결과를 Spring Boot에 전달하고, 향후 Tapo RTSP와 MediaMTX 제어를 담당합니다.

Raspberry Pi Camera Module 기반 MJPEG 프록시와 `/video/*` API는 사용하지 않습니다.

## 현재 API

- `GET /`, `GET /health`
- `POST /register`: Raspberry Pi 장치 등록을 Spring 내부 API로 전달
- `POST /device/{cage_id}/sensor`: 기존 센서 연동 호환 API
- `POST /device/events`: YOLO/비전 처리 결과를 Spring의 이상행동 이벤트 API로 전달
- `GET /internal/cameras/{camera_id}/status`
- `POST /internal/cameras/{camera_id}/live/start`
- `POST /internal/cameras/{camera_id}/live/stop`

카메라 상태 및 live start/stop은 현재 인터페이스 목업입니다. RTSP, FFmpeg, MediaMTX 프로세스를 실제로 시작하지 않습니다.

## 환경변수

```text
SPRING_BOOT_BASE_URL            # 예: https://spring.example.com
SPRING_INTERNAL_API_KEY         # FastAPI -> Spring 내부 호출 키
DEVICE_API_KEY                  # Raspberry Pi -> FastAPI 호출 키
FASTAPI_INTERNAL_API_KEY        # Spring -> FastAPI 내부 호출 키
MEDIAMTX_PLAYBACK_BASE_URL      # 다음 실시간 보기 단계에서 사용
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

실제 RTSP, YOLO, GCS 업로드 기능이 추가되면 해당 기능별 통합 테스트를 추가해야 합니다.
