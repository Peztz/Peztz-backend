package com.peztz.backend.facility.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Facility cage update request")
public record FacilityCageUpdateRequest(
		@Schema(description = "Cage name", example = "Updated Cage")
		@NotBlank
		@Size(max = 100)
		String name,

		@Schema(description = "Cage number", example = "B-2")
		@Size(max = 50)
		String cageNumber,

		@Schema(description = "Connected Raspberry Pi device ID. Blank or null keeps the current device.",
				example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		String raspberryPiDeviceId) {
}
