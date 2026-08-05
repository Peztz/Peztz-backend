package com.peztz.backend.admission.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입실 세션 응답", example = """
		{
		  "sessionId": 1000000002,
		  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23",
		  "accessCode": "123456",
		  "status": "ACTIVE",
		  "startedAt": "2026-06-08T12:00:00",
		  "endedAt": null
		}
		""")
public record AdmissionSessionResponse(
		@Schema(description = "세션 ID", example = "1000000002")
		Long sessionId,

		@Schema(description = "반려동물 ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID petId,

		@Schema(description = "케이지 ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
		UUID cageId,

		@Schema(description = "6자리 접근 코드", example = "\"123456\"")
		String accessCode,

		@Schema(description = "세션 상태", example = "ACTIVE")
		String status,

		@Schema(description = "세션 시작 시각", example = "2026-06-08T12:00:00")
		LocalDateTime startedAt,

		@Schema(description = "세션 종료 시각", example = "2026-06-08T18:00:00", nullable = true)
		LocalDateTime endedAt) {
}
