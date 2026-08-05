package com.peztz.backend.facility.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시설 입실 세션 응답", example = """
		{
		  "sessionId": 1000000002,
		  "petId": "2fa6e09b-8703-44a4-8c30-cf1973e4828f",
		  "petName": "Choco",
		  "cageId": "cbfa50d7-cb89-4951-bad9-5465e85302e9",
		  "cageName": "A-1 Cage",
		  "accessCode": "778416",
		  "status": "ACTIVE",
		  "startedAt": "2026-06-08T16:49:36",
		  "endedAt": null
		}
		""")
public record FacilityAdmissionSessionResponse(
		@Schema(description = "세션 ID", example = "1000000002")
		Long sessionId,

		@Schema(description = "반려동물 ID", example = "2fa6e09b-8703-44a4-8c30-cf1973e4828f")
		UUID petId,

		@Schema(description = "반려동물 이름", example = "Choco")
		String petName,

		@Schema(description = "케이지 ID", example = "cbfa50d7-cb89-4951-bad9-5465e85302e9")
		UUID cageId,

		@Schema(description = "케이지 이름", example = "A-1 Cage")
		String cageName,

		@Schema(description = "6자리 접근 코드", example = "\"778416\"")
		String accessCode,

		@Schema(description = "세션 상태", example = "ACTIVE")
		String status,

		@Schema(description = "세션 시작 시각", example = "2026-06-08T16:49:36")
		LocalDateTime startedAt,

		@Schema(description = "세션 종료 시각", example = "2026-06-08T18:00:00", nullable = true)
		LocalDateTime endedAt) {
}
