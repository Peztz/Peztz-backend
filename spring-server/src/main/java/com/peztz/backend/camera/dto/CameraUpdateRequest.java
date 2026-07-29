package com.peztz.backend.camera.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Camera update request. A camera remains assigned to its original cage.")
public record CameraUpdateRequest(
		@NotBlank
		@Size(max = 100)
		@Schema(description = "Camera display name", example = "Living room camera")
		String name,

		@Size(max = 255)
		@Schema(description = "Reference key for RTSP configuration stored outside API responses")
		String rtspConfigKey) {
}
