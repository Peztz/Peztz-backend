package com.peztz.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login response", example = """
		{
		  "accessToken": "sample-token",
		  "user": {
		    "id": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		    "email": "owner@example.com",
		    "name": "김견주",
		    "role": "OWNER"
		  }
		}
		""")
public record AuthLoginResponse(
		@Schema(description = "Bearer token", example = "sample-token")
		String accessToken,

		@Schema(description = "Logged in user")
		AuthUserResponse user) {
}
