package com.peztz.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request", example = """
		{
		  "email": "owner@example.com",
		  "password": "password1234"
		}
		""")
public record AuthLoginRequest(
		@Schema(description = "Email", example = "owner@example.com")
		@NotBlank @Email
		String email,

		@Schema(description = "Plain password", example = "password1234")
		@NotBlank
		String password) {
}
