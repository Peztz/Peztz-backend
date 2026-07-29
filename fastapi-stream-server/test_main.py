from fastapi.testclient import TestClient

import main


client = TestClient(main.app)


def test_camera_runtime_status_requires_internal_key(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")

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
        "message": "Camera control skeleton only; no RTSP or MediaMTX process was started",
    }


def test_live_start_is_only_a_skeleton(monkeypatch):
    monkeypatch.setattr(main, "FASTAPI_INTERNAL_API_KEY", "test-fastapi-key")

    response = client.post(
        "/internal/cameras/camera-2/live/start",
        headers={"X-Internal-Api-Key": "test-fastapi-key"},
    )
    assert response.status_code == 200
    assert response.json()["status"] == "NOT_IMPLEMENTED"
    assert response.json()["playbackUrl"] is None
