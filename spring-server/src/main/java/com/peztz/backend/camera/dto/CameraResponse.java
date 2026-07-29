package com.peztz.backend.camera.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Camera response. RTSP URL and credentials are never included.")
public record CameraResponse(
		UUID cameraId,
		UUID cageId,
		String cageName,
		String name,
		String status,
		String streamStatus,
		boolean rtspConfigured,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
