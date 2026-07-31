import httpx
from fastapi.testclient import TestClient

import main


client = TestClient(main.app)


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
