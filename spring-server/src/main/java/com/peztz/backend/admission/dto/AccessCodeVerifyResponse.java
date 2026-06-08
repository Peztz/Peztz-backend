package com.peztz.backend.admission.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access code verify response", example = """
		{
		  "valid": true,
		  "sessionId": "3457d769-298f-43d5-a06c-3bbb97bb30d5",
		  "petName": "초코",
		  "cageName": "A-1 케이지",
		  "videoUrl": "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890"
		}
		""")
public record AccessCodeVerifyResponse(
		@Schema(description = "Whether access code is valid", example = "true")
		boolean valid,

		@Schema(description = "Session ID", example = "3457d769-298f-43d5-a06c-3bbb97bb30d5")
		Long sessionId,

		@Schema(description = "Pet name", example = "초코")
		String petName,

		@Schema(description = "Cage name", example = "A-1 케이지")
		String cageName,

		@Schema(description = "FastAPI video proxy URL", example = "http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890")
		String videoUrl) {
}
