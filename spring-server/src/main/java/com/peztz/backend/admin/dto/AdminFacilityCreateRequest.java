package com.peztz.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin facility create request")
public record AdminFacilityCreateRequest(
		@Schema(description = "Facility name", example = "Happy Animal Hospital")
		@NotBlank
		@Size(max = 20)
		String facilityName,

		@Schema(description = "Facility phone number. Blank or null stores '-'.", example = "051-123-4567")
		@Size(max = 20)
		String phoneNumber) {
}
