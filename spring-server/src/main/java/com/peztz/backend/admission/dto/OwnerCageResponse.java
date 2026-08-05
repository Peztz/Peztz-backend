package com.peztz.backend.admission.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "견주의 활성 케이지 응답", example = """
		{
		  "sessionId": 1000000002,
		  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "petName": "Choco",
		  "petBreed": "Poodle",
		  "birthDate": "2026-05-05",
		  "medicalNote": "Shy around strangers",
		  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23",
		  "cageName": "A-1 Cage",
		  "facilityName": "Peztz Busan",
		  "status": "OCCUPIED"
		}
		""")
public record OwnerCageResponse(
		@Schema(description = "세션 ID", example = "1000000002")
		Long sessionId,

		@Schema(description = "반려동물 ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID petId,

		@Schema(description = "반려동물 이름", example = "Choco")
		String petName,

		@Schema(description = "반려동물 품종", example = "Poodle")
		String petBreed,

		@Schema(description = "생년월일", example = "2026-05-05", nullable = true)
		LocalDate birthDate,

		@Schema(description = "의료 참고사항", example = "Shy around strangers", nullable = true)
		String medicalNote,

		@Schema(description = "케이지 ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
		UUID cageId,

		@Schema(description = "케이지 이름", example = "A-1 Cage")
		String cageName,

		@Schema(description = "시설명", example = "Peztz Busan")
		String facilityName,

		@Schema(description = "케이지 상태", example = "OCCUPIED")
		String status) {
}
