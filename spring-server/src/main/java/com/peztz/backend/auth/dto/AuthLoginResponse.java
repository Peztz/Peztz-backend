package com.peztz.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답", example = """
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
		@Schema(description = "Bearer 토큰", example = "sample-token")
		String accessToken,

		@Schema(description = "로그인한 사용자")
		AuthUserResponse user) {
}
