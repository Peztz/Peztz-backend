package com.peztz.backend.facility.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시설용 견주 반려동물 응답", example = """
		{
		  "petId": "2fa6e09b-8703-44a4-8c30-cf1973e4828f",
		  "ownerId": "0684d206-a393-439c-8b6a-1b861a1ca7ec",
		  "ownerEmail": "test@naver.com",
		  "petName": "Choco",
		  "breed": "Poodle",
		  "birthDate": "2022-03-01",
		  "gender": "MALE"
		}
		""")
public record FacilityOwnerPetResponse(
		@Schema(description = "반려동물 ID", example = "2fa6e09b-8703-44a4-8c30-cf1973e4828f")
		UUID petId,

		@Schema(description = "견주 사용자 ID", example = "0684d206-a393-439c-8b6a-1b861a1ca7ec")
		UUID ownerId,

		@Schema(description = "견주 이메일", example = "test@naver.com")
		String ownerEmail,

		@Schema(description = "반려동물 이름", example = "Choco")
		String petName,

		@Schema(description = "반려동물 품종", example = "Poodle")
		String breed,

		@Schema(description = "생년월일", example = "2022-03-01", nullable = true)
		LocalDate birthDate,

		@Schema(description = "성별", example = "MALE", nullable = true)
		String gender) {
}
