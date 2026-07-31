package com.peztz.backend.integration.fastapi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "peztz.fastapi.client-mode", havingValue = "mock", matchIfMissing = true)
public class MockFastApiCameraClient implements FastApiCameraClient {

	private final Map<UUID, String> statuses = new ConcurrentHashMap<>();

	@Override
	public CameraRuntimeStatusResponse getStatus(UUID cameraId) {
		return response(cameraId, statuses.getOrDefault(cameraId, "IDLE"));
	}

	@Override
	public CameraRuntimeStatusResponse startLiveStream(UUID cameraId) {
		statuses.put(cameraId, "MOCK");
		return response(cameraId, "MOCK");
	}

	@Override
	public CameraRuntimeStatusResponse stopLiveStream(UUID cameraId) {
		statuses.put(cameraId, "IDLE");
		return response(cameraId, "IDLE");
	}

	private CameraRuntimeStatusResponse response(UUID cameraId, String status) {
		return new CameraRuntimeStatusResponse(
				cameraId,
				status,
				null,
				"Mock adapter only; no RTSP or MediaMTX process was started");
	}
}
