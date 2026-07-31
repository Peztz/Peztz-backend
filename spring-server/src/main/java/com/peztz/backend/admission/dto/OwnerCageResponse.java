package com.peztz.backend.admission.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Owner active cage response", example = """
		{
		  "sessionId": 1000000002,
		  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "petName": "Choco",
		  "petBreed": "Poodle",
		  "birthDate": "2026-05-05",
		  "medicalNote": "Shy around strangers",
		  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23",
		  "cageName": "A-1 Cage",
		  "facilityName": "Peztz Busan",
		  "status": "OCCUPIED"
		}
		""")
public record OwnerCageResponse(
		@Schema(description = "Session ID", example = "1000000002")
		Long sessionId,

		@Schema(description = "Pet ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID petId,

		@Schema(description = "Pet name", example = "Choco")
		String petName,

		@Schema(description = "Pet breed", example = "Poodle")
		String petBreed,

		@Schema(description = "Birth date", example = "2026-05-05", nullable = true)
		LocalDate birthDate,

		@Schema(description = "Medical note", example = "Shy around strangers", nullable = true)
		String medicalNote,

		@Schema(description = "Cage ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
		UUID cageId,

		@Schema(description = "Cage name", example = "A-1 Cage")
		String cageName,

		@Schema(description = "Facility name", example = "Peztz Busan")
		String facilityName,

		@Schema(description = "Cage status", example = "OCCUPIED")
		String status) {
}
