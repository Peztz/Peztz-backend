package com.peztz.backend.pet.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "반려동물 생성 및 수정 요청", example = """
		{
		  "name": "초코",
		  "species": "DOG",
		  "breed": "푸들",
		  "gender": "MALE",
		  "birthDate": "2022-03-01",
		  "weightKg": 5.2,
		  "memo": "겁이 조금 많음"
		}
		""")
public record PetRequest(
		@Schema(description = "반려동물 이름", example = "초코")
		@NotBlank
		String name,

		@Schema(description = "동물 종", example = "DOG")
		String species,

		@Schema(description = "품종", example = "푸들")
		@NotBlank
		String breed,

		@Schema(description = "성별", example = "MALE")
		String gender,

		@Schema(description = "생년월일", example = "2022-03-01")
		LocalDate birthDate,

		@Schema(description = "체중(kg)", example = "5.2")
		Double weightKg,

		@Schema(description = "메모", example = "겁이 조금 많음")
		String memo) {
}
