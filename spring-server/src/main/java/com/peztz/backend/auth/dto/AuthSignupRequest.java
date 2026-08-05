package com.peztz.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청", example = """
		{
		  "email": "owner@example.com",
		  "password": "password1234",
		  "name": "김견주",
		  "phoneNumber": "010-1234-5678",
		  "role": "OWNER"
		}
		""")
public record AuthSignupRequest(
		@Schema(description = "이메일", example = "owner@example.com")
		@NotBlank @Email
		String email,

		@Schema(description = "평문 비밀번호입니다. BCrypt 해시로 저장됩니다.", example = "password1234")
		@NotBlank @Size(min = 8)
		String password,

		@Schema(description = "사용자 이름", example = "김견주")
		@NotBlank
		String name,

		@Schema(description = "전화번호", example = "010-1234-5678")
		String phoneNumber,

		@Schema(description = "사용자 역할", example = "OWNER", allowableValues = {"OWNER", "FACILITY_MANAGER", "ADMIN"})
		@NotBlank
		String role) {
}
