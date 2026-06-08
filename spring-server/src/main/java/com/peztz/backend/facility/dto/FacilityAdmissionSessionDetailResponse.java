package com.peztz.backend.facility.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Facility admission session detail response", example = """
		{
		  "sessionId": 1000000003,
		  "petId": "26f294e7-bb04-4fc0-bca7-f8d014d4dc29",
		  "petName": "Choco",
		  "ownerId": "0684d206-a393-439c-8b6a-1b861a1ca7ec",
		  "ownerEmail": "test@naver.com",
		  "cageId": "b228a4ed-7842-430a-a052-259f2bab9d70",
		  "cageName": "Facility Test Cage",
		  "cageNumber": "F-1",
		  "accessCode": "994413",
		  "status": "ACTIVE",
		  "startedAt": "2026-06-08T17:57:45",
		  "endedAt": null,
		  "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890"
		}
		""")
public record FacilityAdmissionSessionDetailResponse(
		@Schema(description = "Session ID", example = "1000000003")
		Long sessionId,

		@Schema(description = "Pet ID", example = "26f294e7-bb04-4fc0-bca7-f8d014d4dc29")
		UUID petId,

		@Schema(description = "Pet name", example = "Choco")
		String petName,

		@Schema(description = "Owner user ID", example = "0684d206-a393-439c-8b6a-1b861a1ca7ec")
		UUID ownerId,

		@Schema(description = "Owner email", example = "test@naver.com")
		String ownerEmail,

		@Schema(description = "Cage ID", example = "b228a4ed-7842-430a-a052-259f2bab9d70")
		UUID cageId,

		@Schema(description = "Cage name", example = "Facility Test Cage")
		String cageName,

		@Schema(description = "Cage number", example = "F-1")
		String cageNumber,

		@Schema(description = "Six digit access code", example = "\"994413\"")
		String accessCode,

		@Schema(description = "Session status", example = "ACTIVE")
		String status,

		@Schema(description = "Session start time", example = "2026-06-08T17:57:45")
		LocalDateTime startedAt,

		@Schema(description = "Session end time", example = "2026-06-08T18:15:54", nullable = true)
		LocalDateTime endedAt,

		@Schema(description = "FastAPI video proxy URL", example = "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890")
		String videoUrl) {
}
