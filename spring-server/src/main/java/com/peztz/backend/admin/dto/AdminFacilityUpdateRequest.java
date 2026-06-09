package com.peztz.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin facility update request")
public record AdminFacilityUpdateRequest(
		@Schema(description = "Facility name", example = "Updated Animal Hospital")
		@NotBlank
		@Size(max = 20)
		String facilityName,

		@Schema(description = "Facility phone number. Blank or null stores '-'.", example = "051-999-9999")
		@Size(max = 20)
		String phoneNumber) {
}
