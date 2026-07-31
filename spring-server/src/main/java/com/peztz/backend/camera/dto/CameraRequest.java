package com.peztz.backend.camera.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Camera create request")
public record CameraRequest(
		@NotNull
		@Schema(description = "Cage ID owned by the authenticated user. Only one camera can be registered per cage.")
		UUID cageId,

		@NotBlank
		@Size(max = 100)
		@Schema(description = "Camera display name", example = "Living room camera")
		String name,

		@Size(max = 255)
		@Schema(description = "Reference key for RTSP configuration stored outside API responses")
		String rtspConfigKey) {
}
