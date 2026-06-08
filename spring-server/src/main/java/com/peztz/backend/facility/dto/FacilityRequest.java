package com.peztz.backend.facility.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Facility create request", example = """
		{
		  "name": "Peztz 부산점",
		  "address": "부산광역시 해운대구 센텀중앙로 1",
		  "phoneNumber": "051-123-4567"
		}
		""")
public record FacilityRequest(
		@Schema(description = "Facility name", example = "Peztz 부산점")
		@NotBlank
		String name,

		@Schema(description = "Facility address", example = "부산광역시 해운대구 센텀중앙로 1")
		String address,

		@Schema(description = "Facility phone number", example = "051-123-4567")
		@NotBlank
		String phoneNumber) {
}
