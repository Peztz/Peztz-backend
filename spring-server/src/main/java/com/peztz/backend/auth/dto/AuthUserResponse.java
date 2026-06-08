package com.peztz.backend.auth.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authenticated user response", example = """
		{
		  "id": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "email": "owner@example.com",
		  "name": "김견주",
		  "role": "OWNER"
		}
		""")
public record AuthUserResponse(
		@Schema(description = "User ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID id,

		@Schema(description = "Email", example = "owner@example.com")
		String email,

		@Schema(description = "User name", example = "김견주")
		String name,

		@Schema(description = "Role", example = "OWNER")
		String role) {
}
