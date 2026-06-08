package com.peztz.backend.pet.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Pet create/update request", example = """
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
		@Schema(description = "Pet name", example = "초코")
		@NotBlank
		String name,

		@Schema(description = "Species", example = "DOG")
		String species,

		@Schema(description = "Breed", example = "푸들")
		@NotBlank
		String breed,

		@Schema(description = "Gender", example = "MALE")
		String gender,

		@Schema(description = "Birth date", example = "2022-03-01")
		LocalDate birthDate,

		@Schema(description = "Weight in kilograms", example = "5.2")
		Double weightKg,

		@Schema(description = "Memo", example = "겁이 조금 많음")
		String memo) {
}
