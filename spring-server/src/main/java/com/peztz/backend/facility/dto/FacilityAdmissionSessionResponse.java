package com.peztz.backend.facility.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Facility admission session response", example = """
		{
		  "sessionId": 1000000002,
		  "petId": "2fa6e09b-8703-44a4-8c30-cf1973e4828f",
		  "petName": "Choco",
		  "cageId": "cbfa50d7-cb89-4951-bad9-5465e85302e9",
		  "cageName": "A-1 Cage",
		  "accessCode": "778416",
		  "status": "ACTIVE",
		  "startedAt": "2026-06-08T16:49:36",
		  "endedAt": null,
		  "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890"
		}
		""")
public record FacilityAdmissionSessionResponse(
		@Schema(description = "Session ID", example = "1000000002")
		Long sessionId,

		@Schema(description = "Pet ID", example = "2fa6e09b-8703-44a4-8c30-cf1973e4828f")
		UUID petId,

		@Schema(description = "Pet name", example = "Choco")
		String petName,

		@Schema(description = "Cage ID", example = "cbfa50d7-cb89-4951-bad9-5465e85302e9")
		UUID cageId,

		@Schema(description = "Cage name", example = "A-1 Cage")
		String cageName,

		@Schema(description = "Six digit access code", example = "\"778416\"")
		String accessCode,

		@Schema(description = "Session status", example = "ACTIVE")
		String status,

		@Schema(description = "Session start time", example = "2026-06-08T16:49:36")
		LocalDateTime startedAt,

		@Schema(description = "Session end time", example = "2026-06-08T18:00:00", nullable = true)
		LocalDateTime endedAt,

		@Schema(description = "FastAPI video proxy URL", example = "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890")
		String videoUrl) {
}
