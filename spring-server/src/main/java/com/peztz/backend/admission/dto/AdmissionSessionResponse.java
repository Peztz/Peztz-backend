package com.peztz.backend.admission.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admission session response", example = """
		{
		  "sessionId": "3457d769-298f-43d5-a06c-3bbb97bb30d5",
		  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23",
		  "accessCode": "123456",
		  "status": "ACTIVE",
		  "startedAt": "2026-06-08T12:00:00",
		  "endedAt": null,
		  "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890"
		}
		""")
public record AdmissionSessionResponse(
		@Schema(description = "Session ID", example = "3457d769-298f-43d5-a06c-3bbb97bb30d5")
		Long sessionId,

		@Schema(description = "Pet ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID petId,

		@Schema(description = "Cage ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
		UUID cageId,

		@Schema(description = "Six digit access code", example = "123456")
		String accessCode,

		@Schema(description = "Session status", example = "ACTIVE")
		String status,

		@Schema(description = "Session start time", example = "2026-06-08T12:00:00")
		LocalDateTime startedAt,

		@Schema(description = "Session end time", example = "2026-06-08T18:00:00", nullable = true)
		LocalDateTime endedAt,

		@Schema(description = "FastAPI video proxy URL", example = "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890")
		String videoUrl) {
}
