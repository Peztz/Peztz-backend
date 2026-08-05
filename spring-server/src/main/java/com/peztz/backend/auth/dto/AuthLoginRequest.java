package com.peztz.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청", example = """
		{
		  "email": "owner@example.com",
		  "password": "password1234"
		}
		""")
public record AuthLoginRequest(
		@Schema(description = "이메일", example = "owner@example.com")
		@NotBlank @Email
		String email,

		@Schema(description = "평문 비밀번호", example = "password1234")
		@NotBlank
		String password) {
}
