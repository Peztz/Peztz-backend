package com.peztz.backend.pet.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "반려동물 응답", example = """
		{
		  "id": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "ownerId": "4b6ed63b-e0cb-45dd-bff8-d66f09ef9a31",
		  "name": "초코",
		  "species": "DOG",
		  "breed": "푸들",
		  "gender": "MALE",
		  "birthDate": "2022-03-01",
		  "weightKg": 5.2,
		  "memo": "겁이 조금 많음",
		  "createdAt": "2026-06-08T12:00:00"
		}
		""")
public record PetResponse(
		@Schema(description = "반려동물 ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID id,

		@Schema(description = "견주 사용자 ID", example = "4b6ed63b-e0cb-45dd-bff8-d66f09ef9a31")
		UUID ownerId,

		@Schema(description = "반려동물 이름", example = "초코")
		String name,

		@Schema(description = "동물 종", example = "DOG")
		String species,

		@Schema(description = "품종", example = "푸들")
		String breed,

		@Schema(description = "성별", example = "MALE")
		String gender,

		@Schema(description = "생년월일", example = "2022-03-01")
		LocalDate birthDate,

		@Schema(description = "체중(kg)", example = "5.2")
		Double weightKg,

		@Schema(description = "메모", example = "겁이 조금 많음")
		String memo,

		@Schema(description = "생성 시각", example = "2026-06-08T12:00:00")
		LocalDateTime createdAt) {
}
