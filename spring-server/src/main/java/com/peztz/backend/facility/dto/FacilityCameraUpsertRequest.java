package com.peztz.backend.facility.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Facility camera registration request")
public record FacilityCameraUpsertRequest(
		@NotBlank
		@Size(max = 100)
		@Schema(description = "Camera display name", example = "Demo camera")
		String name) {
}
