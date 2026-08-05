package com.peztz.backend.auth.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증된 사용자 응답", example = """
		{
		  "id": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "email": "owner@example.com",
		  "name": "김견주",
		  "role": "OWNER"
		}
		""")
public record AuthUserResponse(
		@Schema(description = "사용자 ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID id,

		@Schema(description = "이메일", example = "owner@example.com")
		String email,

		@Schema(description = "사용자 이름", example = "김견주")
		String name,

		@Schema(description = "사용자 역할", example = "OWNER")
		String role) {
}
