package com.peztz.backend.facility.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Facility owner pet response", example = """
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
		@Schema(description = "Pet ID", example = "2fa6e09b-8703-44a4-8c30-cf1973e4828f")
		UUID petId,

		@Schema(description = "Owner user ID", example = "0684d206-a393-439c-8b6a-1b861a1ca7ec")
		UUID ownerId,

		@Schema(description = "Owner email", example = "test@naver.com")
		String ownerEmail,

		@Schema(description = "Pet name", example = "Choco")
		String petName,

		@Schema(description = "Pet breed", example = "Poodle")
		String breed,

		@Schema(description = "Birth date", example = "2022-03-01", nullable = true)
		LocalDate birthDate,

		@Schema(description = "Gender", example = "MALE", nullable = true)
		String gender) {
}
