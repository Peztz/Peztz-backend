package com.peztz.backend.cage.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cage response", example = """
		{
		  "id": "d69fc7ff-481c-4305-b81c-551955a1ce23",
		  "facilityId": "0e96bc6a-90a5-45cc-ac64-37d19254e7a2",
		  "name": "A-1 케이지",
		  "cageNumber": "A-1",
		  "status": "AVAILABLE",
		  "raspberryPiDeviceId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "createdAt": "2026-06-08T12:00:00"
		}
		""")
public record CageResponse(
		@Schema(description = "Cage ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
		UUID id,

		@Schema(description = "Facility ID", example = "0e96bc6a-90a5-45cc-ac64-37d19254e7a2")
		UUID facilityId,

		@Schema(description = "Cage name", example = "A-1 케이지")
		String name,

		@Schema(description = "Cage number", example = "A-1")
		String cageNumber,

		@Schema(description = "Cage status", example = "AVAILABLE")
		String status,

		@Schema(description = "Connected Raspberry Pi device ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID raspberryPiDeviceId,

		@Schema(description = "Created date time", example = "2026-06-08T12:00:00")
		LocalDateTime createdAt) {
}
