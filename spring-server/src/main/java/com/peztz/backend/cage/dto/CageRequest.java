package com.peztz.backend.cage.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Cage create/update request", example = """
		{
		  "name": "A-1 케이지",
		  "cageNumber": "A-1",
		  "status": "AVAILABLE",
		  "raspberryPiDeviceId": "7bf2b0d2-dd67-4002-929a-d4505f6af890"
		}
		""")
public record CageRequest(
		@Schema(description = "Cage name", example = "A-1 케이지")
		String name,

		@Schema(description = "Cage number", example = "A-1")
		String cageNumber,

		@Schema(description = "Cage status", example = "AVAILABLE", allowableValues = {"AVAILABLE", "OCCUPIED", "MAINTENANCE"})
		@NotBlank
		String status,

		@Schema(description = "Connected Raspberry Pi device ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID raspberryPiDeviceId) {
}
