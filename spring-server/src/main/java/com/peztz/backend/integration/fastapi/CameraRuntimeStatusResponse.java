package com.peztz.backend.integration.fastapi;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Camera runtime status returned by the FastAPI adapter")
public record CameraRuntimeStatusResponse(
		UUID cameraId,
		String status,
		String playbackUrl,
		String message) {
}
