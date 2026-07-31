package com.peztz.backend.admission.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access code verify response", example = """
		{
		  "valid": true,
		  "sessionId": 1000000002,
		  "petName": "초코",
		  "cageName": "A-1 케이지"
		}
		""")
public record AccessCodeVerifyResponse(
		@Schema(description = "Whether access code is valid", example = "true")
		boolean valid,

		@Schema(description = "Session ID", example = "1000000002")
		Long sessionId,

		@Schema(description = "Pet name", example = "초코")
		String petName,

		@Schema(description = "Cage name", example = "A-1 케이지")
		String cageName) {
}
