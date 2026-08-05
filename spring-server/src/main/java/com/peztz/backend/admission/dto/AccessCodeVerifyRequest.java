package com.peztz.backend.admission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "접근 코드 인증 요청", example = """
		{
		  "accessCode": "123456"
		}
		""")
public record AccessCodeVerifyRequest(
		@Schema(description = "6자리 접근 코드", example = "\"123456\"")
		@NotBlank
		String accessCode) {
}
