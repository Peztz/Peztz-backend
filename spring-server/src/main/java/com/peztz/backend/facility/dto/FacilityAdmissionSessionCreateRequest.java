package com.peztz.backend.facility.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "시설 입실 세션 생성 요청", example = """
		{
		  "ownerEmail": "test@naver.com",
		  "petId": "2fa6e09b-8703-44a4-8c30-cf1973e4828f",
		  "cageId": "cbfa50d7-cb89-4951-bad9-5465e85302e9"
		}
		""")
public record FacilityAdmissionSessionCreateRequest(
		@Schema(description = "견주 이메일", example = "test@naver.com")
		@NotBlank @Email
		String ownerEmail,

		@Schema(description = "반려동물 ID", example = "2fa6e09b-8703-44a4-8c30-cf1973e4828f")
		@NotNull
		UUID petId,

		@Schema(description = "케이지 ID", example = "cbfa50d7-cb89-4951-bad9-5465e85302e9")
		@NotNull
		UUID cageId) {
}
