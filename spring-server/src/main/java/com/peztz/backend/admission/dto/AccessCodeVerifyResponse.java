package com.peztz.backend.admission.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "접근 코드 인증 응답", example = """
		{
		  "valid": true,
		  "sessionId": 1000000002,
		  "petName": "초코",
		  "cageName": "A-1 케이지"
		}
		""")
public record AccessCodeVerifyResponse(
		@Schema(description = "접근 코드 유효 여부", example = "true")
		boolean valid,

		@Schema(description = "세션 ID", example = "1000000002")
		Long sessionId,

		@Schema(description = "반려동물 이름", example = "초코")
		String petName,

		@Schema(description = "케이지 이름", example = "A-1 케이지")
		String cageName) {
}
