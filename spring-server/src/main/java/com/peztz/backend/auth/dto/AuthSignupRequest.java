package com.peztz.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Signup request", example = """
		{
		  "email": "owner@example.com",
		  "password": "password1234",
		  "name": "김견주",
		  "phoneNumber": "010-1234-5678",
		  "role": "OWNER"
		}
		""")
public record AuthSignupRequest(
		@Schema(description = "Email", example = "owner@example.com")
		@NotBlank @Email
		String email,

		@Schema(description = "Plain password. It is stored as BCrypt hash.", example = "password1234")
		@NotBlank @Size(min = 8)
		String password,

		@Schema(description = "User name", example = "김견주")
		@NotBlank
		String name,

		@Schema(description = "Phone number", example = "010-1234-5678")
		String phoneNumber,

		@Schema(description = "Role", example = "OWNER", allowableValues = {"OWNER", "FACILITY_MANAGER", "ADMIN"})
		@NotBlank
		String role) {
}
