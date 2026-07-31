package com.peztz.backend.integration.fastapi;

import java.util.UUID;

public interface FastApiCameraClient {

	CameraRuntimeStatusResponse getStatus(UUID cameraId);

	CameraRuntimeStatusResponse startLiveStream(UUID cameraId);

	CameraRuntimeStatusResponse stopLiveStream(UUID cameraId);
}
