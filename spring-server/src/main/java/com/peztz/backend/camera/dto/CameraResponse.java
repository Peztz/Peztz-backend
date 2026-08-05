package com.peztz.backend.camera.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카메라 응답입니다. RTSP URL과 인증 정보는 포함하지 않습니다.")
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
