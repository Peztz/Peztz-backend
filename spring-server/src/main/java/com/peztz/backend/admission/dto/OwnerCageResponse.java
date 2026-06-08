package com.peztz.backend.admission.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Owner active cage response", example = """
		{
		  "sessionId": "3457d769-298f-43d5-a06c-3bbb97bb30d5",
		  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "petName": "초코",
		  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23",
		  "cageName": "A-1 케이지",
		  "facilityName": "Peztz 부산점",
		  "status": "OCCUPIED",
		  "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890"
		}
		""")
public record OwnerCageResponse(
		@Schema(description = "Session ID", example = "3457d769-298f-43d5-a06c-3bbb97bb30d5")
		Long sessionId,

		@Schema(description = "Pet ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID petId,

		@Schema(description = "Pet name", example = "초코")
		String petName,

		@Schema(description = "Cage ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
		UUID cageId,

		@Schema(description = "Cage name", example = "A-1 케이지")
		String cageName,

		@Schema(description = "Facility name", example = "Peztz 부산점")
		String facilityName,

		@Schema(description = "Cage status", example = "OCCUPIED")
		String status,

		@Schema(description = "FastAPI video proxy URL", example = "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890")
		String videoUrl) {
}
