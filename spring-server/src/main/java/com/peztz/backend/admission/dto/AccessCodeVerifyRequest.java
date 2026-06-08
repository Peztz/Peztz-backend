package com.peztz.backend.admission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Access code verify request", example = """
		{
		  "accessCode": "123456"
		}
		""")
public record AccessCodeVerifyRequest(
		@Schema(description = "Six digit access code", example = "\"123456\"")
		@NotBlank
		String accessCode) {
}
