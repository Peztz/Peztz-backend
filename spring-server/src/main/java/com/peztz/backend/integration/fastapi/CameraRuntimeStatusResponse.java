package com.peztz.backend.integration.fastapi;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FastAPI 어댑터가 반환하는 카메라 실행 상태")
public record CameraRuntimeStatusResponse(
		UUID cameraId,
		String status,
		String playbackUrl,
		String message) {
}
