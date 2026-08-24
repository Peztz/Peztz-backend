import json

import httpx
from fastapi.testclient import TestClient

import main
import report_prompt


client = TestClient(main.app)


def test_daily_report_prompt_is_versioned_and_preserves_payload():
    messages = report_prompt.build_daily_report_messages(
        {"petName": "초코", "statistics": {"totalLogCount": 2}}
    )

    assert report_prompt.DAILY_REPORT_PROMPT_VERSION == "daily-report-v2"
    assert messages[0]["role"] == "system"
    assert "진단" in messages[0]["content"]
    assert "daily-report-v2" in messages[1]["content"]
    assert "Asia/Seoul(UTC+9)" in messages[0]["content"]
    assert "긴 ISO 8601 문자열" in messages[0]["content"]
    assert '"petName": "초코"' in messages[1]["content"]


def test_device_event_without_pet_id_forwards_to_spring(monkeypatch):
    monkeypatch.setattr(main, "DEVICE_API_KEY", "test-device-key")
    monkeypatch.setattr(main, "SPRING_BOOT_BASE_URL", "http://spring:8080")
    monkeypatch.setattr(main, "SPRING_INTERNAL_API_KEY", "test-spring-key")

    original_async_client = main.httpx.AsyncClient

    def handler(request):
        assert request.url == httpx.URL("http://spring:8080/api/internal/pet-events")
        assert request.headers["X-Internal-Api-Key"] == "test-spring-key"
        payload = json.loads(request.content)
        assert payload == {
            "externalEventId": "edge-event-001",
            "cameraId": "7be9fca6-f4e0-40b8-bfa8-30384bdcb471",
            "eventType": "PACING",
            "confidence": 0.91,
            "occurredAt": "2026-08-18T01:20:30+00:00",
            "metadata": {
                "detectedAnimalType": "DOG",
                "frameId": 1234,
            },
        }
        return httpx.Response(200, json={"eventId": 42, "eventType": "PACING"})

    def async_client(*args, **kwargs):
        kwargs["transport"] = httpx.MockTransport(handler)
        return original_async_client(*args, **kwargs)

    monkeypatch.setattr(main.httpx, "AsyncClient", async_client)

    unauthorized = client.post(
        "/device/events",
        json={
            "externalEventId": "edge-event-001",
            "cameraId": "7be9fca6-f4e0-40b8-bfa8-30384bdcb471",
            "eventType": "PACING",
            "confidence": 0.91,
            "occurredAt": "2026-08-18T01:20:30+00:00",
        },
    )
    assert unauthorized.status_code == 401

    response = client.post(
        "/device/events",
        headers={"X-Device-Api-Key": "test-device-key"},
        json={
            "externalEventId": "edge-event-001",
            "cameraId": "7be9fca6-f4e0-40b8-bfa8-30384bdcb471",
            "eventType": "PACING",
            "confidence": 0.91,
            "occurredAt": "2026-08-18T01:20:30+00:00",
            "metadata": {
                "detectedAnimalType": "DOG",
                "frameId": 1234,
            },
        },
    )

    assert response.status_code == 200
    assert response.json() == {"eventId": 42, "eventType": "PACING"}


def test_camera_runtime_status_requires_internal_key_and_reports_offline(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")
    monkeypatch.setattr(main, "MEDIAMTX_STREAM_PATH", "cage-a1")

    async def offline(_stream_path):
        return False

    monkeypatch.setattr(main, "_mediamtx_path_is_online", offline)

    unauthorized = client.get("/internal/cameras/camera-1/status")
    assert unauthorized.status_code == 401

    response = client.get(
        "/internal/cameras/camera-1/status",
        headers={"X-Internal-Api-Key": "test-fastapi-key"},
    )
    assert response.status_code == 200
    assert response.json() == {
        "cameraId": "camera-1",
        "status": "IDLE",
        "playbackUrl": None,
        "message": "Raspberry Pi is not publishing this camera stream",
    }


def test_camera_runtime_status_returns_playback_url_when_online(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")
    monkeypatch.setattr(main, "MEDIAMTX_STREAM_PATH", "cage-a1")
    monkeypatch.setattr(main, "MEDIAMTX_PLAYBACK_BASE_URL", "http://media.example:8889")

    async def online(_stream_path):
        return True

    monkeypatch.setattr(main, "_mediamtx_path_is_online", online)

    response = client.get(
        "/internal/cameras/camera-2/status",
        headers={"X-Internal-Api-Key": "test-fastapi-key"},
    )
    assert response.status_code == 200
    assert response.json() == {
        "cameraId": "camera-2",
        "status": "ONLINE",
        "playbackUrl": "http://media.example:8889/cage-a1/",
        "message": "Camera stream is online",
    }


def test_camera_runtime_status_reads_mediamtx_control_api(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")
    monkeypatch.setattr(main, "MEDIAMTX_API_BASE_URL", "http://127.0.0.1:9997")
    monkeypatch.setattr(main, "MEDIAMTX_STREAM_PATH", "cage-a1")
    monkeypatch.setattr(main, "MEDIAMTX_PLAYBACK_BASE_URL", "http://media.example:8889")

    original_async_client = main.httpx.AsyncClient

    def handler(request):
        assert request.url.path == "/v3/paths/get/cage-a1"
        return httpx.Response(200, json={"name": "cage-a1", "ready": True})

    def async_client(*args, **kwargs):
        kwargs["transport"] = httpx.MockTransport(handler)
        return original_async_client(*args, **kwargs)

    monkeypatch.setattr(main.httpx, "AsyncClient", async_client)

    response = client.get(
        "/internal/cameras/camera-3/status",
        headers={"X-Internal-Api-Key": "test-fastapi-key"},
    )
    assert response.status_code == 200
    assert response.json()["status"] == "ONLINE"
    assert response.json()["playbackUrl"] == "http://media.example:8889/cage-a1/"


def test_camera_runtime_status_rejects_invalid_mediamtx_response(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")
    monkeypatch.setattr(main, "MEDIAMTX_API_BASE_URL", "http://127.0.0.1:9997")
    monkeypatch.setattr(main, "MEDIAMTX_STREAM_PATH", "cage-a1")

    original_async_client = main.httpx.AsyncClient

    def handler(_request):
        return httpx.Response(200, json=[])

    def async_client(*args, **kwargs):
        kwargs["transport"] = httpx.MockTransport(handler)
        return original_async_client(*args, **kwargs)

    monkeypatch.setattr(main.httpx, "AsyncClient", async_client)

    response = client.get(
        "/internal/cameras/camera-4/status",
        headers={"X-Internal-Api-Key": "test-fastapi-key"},
    )
    assert response.status_code == 502
    assert response.json()["detail"] == "MediaMTX API returned an invalid response"


def test_live_start_is_managed_by_raspberry_pi(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")

    response = client.post(
        "/internal/cameras/camera-2/live/start",
        headers={"X-Internal-Api-Key": "test-fastapi-key"},
    )
    assert response.status_code == 409
    assert response.json()["detail"] == "Live stream is managed by the Raspberry Pi systemd service"


def test_daily_report_requires_internal_key(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")
    monkeypatch.setattr(main, "OPENAI_API_KEY", "test-openai-key")

    response = client.post(
        "/internal/reports/daily/generate",
        json={
            "reportDate": "2026-08-22",
            "petName": "초코",
            "statistics": {
                "totalLogCount": 0,
                "sensorLogCount": 0,
            },
            "events": [],
        },
    )

    assert response.status_code == 401


def test_daily_report_returns_structured_cards(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")
    monkeypatch.setattr(main, "OPENAI_API_KEY", "test-openai-key")

    def generate(request):
        assert request.petName == "초코"
        assert request.statistics.totalLogCount == 2
        assert request.events[0].type == "DOOR_OPEN"
        return main.DailyReportGenerationResponse(
            summary="오늘은 전반적으로 안정적인 하루였습니다.",
            behaviorCards=[
                main.BehaviorCard(
                    title="문 열림 감지",
                    description="문 열림이 한 차례 감지되었습니다.",
                    evidence=["09:10 DOOR_OPEN"],
                )
            ],
            environmentCard=main.EnvironmentCard(
                title="생활 환경",
                description="평균 온도와 습도가 안정적이었습니다.",
            ),
            careTips=["물을 충분히 마시는지 확인해 주세요."],
            riskLevel=main.RiskLevel.NORMAL,
            warnings=[],
            disclaimer="이 리포트는 진단이 아닌 관찰 데이터 요약입니다.",
        )

    monkeypatch.setattr(main, "_generate_openai_daily_report", generate)

    response = client.post(
        "/internal/reports/daily/generate",
        headers={"X-Internal-Api-Key": "test-fastapi-key"},
        json={
            "reportDate": "2026-08-22",
            "petName": "초코",
            "breed": "Poodle",
            "birthDate": "2022-03-01",
            "statistics": {
                "totalLogCount": 2,
                "sensorLogCount": 1,
                "averageTemperature": 25.2,
                "averageHumidity": 61.0,
                "doorOpenCount": 1,
                "lowLightCount": 0,
            },
            "events": [
                {
                    "type": "DOOR_OPEN",
                    "occurredAt": "2026-08-22T09:10:00+09:00",
                    "message": "케이지 문 열림",
                }
            ],
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["riskLevel"] == "NORMAL"
    assert payload["behaviorCards"][0]["title"] == "문 열림 감지"
    assert payload["environmentCard"]["title"] == "생활 환경"


def test_daily_report_reports_missing_openai_key(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")
    monkeypatch.setattr(main, "OPENAI_API_KEY", "")

    response = client.post(
        "/internal/reports/daily/generate",
        headers={"X-Internal-Api-Key": "test-fastapi-key"},
        json={
            "reportDate": "2026-08-22",
            "petName": "초코",
            "statistics": {
                "totalLogCount": 0,
                "sensorLogCount": 0,
            },
        },
    )

    assert response.status_code == 503
    assert response.json()["detail"] == "OpenAI API key is not configured"
