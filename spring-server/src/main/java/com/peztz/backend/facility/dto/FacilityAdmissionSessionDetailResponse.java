package com.peztz.backend.facility.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시설 입실 세션 상세 응답", example = """
		{
		  "sessionId": 1000000003,
		  "petId": "26f294e7-bb04-4fc0-bca7-f8d014d4dc29",
		  "petName": "Choco",
		  "ownerId": "0684d206-a393-439c-8b6a-1b861a1ca7ec",
		  "ownerEmail": "test@naver.com",
		  "cageId": "b228a4ed-7842-430a-a052-259f2bab9d70",
		  "cageName": "Facility Test Cage",
		  "cageNumber": "F-1",
		  "accessCode": "994413",
		  "status": "ACTIVE",
		  "startedAt": "2026-06-08T17:57:45",
		  "endedAt": null
		}
		""")
public record FacilityAdmissionSessionDetailResponse(
		@Schema(description = "세션 ID", example = "1000000003")
		Long sessionId,

		@Schema(description = "반려동물 ID", example = "26f294e7-bb04-4fc0-bca7-f8d014d4dc29")
		UUID petId,

		@Schema(description = "반려동물 이름", example = "Choco")
		String petName,

		@Schema(description = "견주 사용자 ID", example = "0684d206-a393-439c-8b6a-1b861a1ca7ec")
		UUID ownerId,

		@Schema(description = "견주 이메일", example = "test@naver.com")
		String ownerEmail,

		@Schema(description = "케이지 ID", example = "b228a4ed-7842-430a-a052-259f2bab9d70")
		UUID cageId,

		@Schema(description = "케이지 이름", example = "Facility Test Cage")
		String cageName,

		@Schema(description = "케이지 번호", example = "F-1")
		String cageNumber,

		@Schema(description = "6자리 접근 코드", example = "\"994413\"")
		String accessCode,

		@Schema(description = "세션 상태", example = "ACTIVE")
		String status,

		@Schema(description = "세션 시작 시각", example = "2026-06-08T17:57:45")
		LocalDateTime startedAt,

		@Schema(description = "세션 종료 시각", example = "2026-06-08T18:15:54", nullable = true)
		LocalDateTime endedAt) {
}
