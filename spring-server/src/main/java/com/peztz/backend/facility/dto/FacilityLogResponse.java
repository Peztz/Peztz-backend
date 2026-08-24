package com.peztz.backend.facility.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시설 운영 로그 응답", example = """
		{
		  "logId": 136,
		  "sessionId": 1000000010,
		  "cageId": "4bcbf052-417e-45c5-86f2-e54afdb9235d",
		  "cageName": "뉴케이지",
		  "cageNumber": "B-1",
		  "petId": "8509f693-9a74-435b-973e-da255f5c064b",
		  "petName": "마루",
		  "type": "DOOR_OPEN",
		  "category": "SENSOR",
		  "level": "WARNING",
		  "message": "Cage door opened",
		  "createdAt": "2026-08-24T21:00:59+09:00"
		}
		""")
public record FacilityLogResponse(
		@Schema(description = "로그 ID", example = "136")
		Long logId,

		@Schema(description = "입실 세션 ID", example = "1000000010")
		Long sessionId,

		@Schema(description = "케이지 ID")
		UUID cageId,

		@Schema(description = "케이지 이름", example = "뉴케이지")
		String cageName,

		@Schema(description = "케이지 번호", example = "B-1")
		String cageNumber,

		@Schema(description = "반려동물 ID")
		UUID petId,

		@Schema(description = "반려동물 이름", example = "마루")
		String petName,

		@Schema(description = "원본 이벤트 타입", example = "DOOR_OPEN")
		String type,

		@Schema(description = "화면 집계용 분류", example = "SENSOR",
				allowableValues = {"SENSOR", "BEHAVIOR", "ACCESS", "SESSION", "NETWORK", "OTHER"})
		String category,

		@Schema(description = "표시 등급", example = "WARNING", allowableValues = {"NORMAL", "WARNING"})
		String level,

		@Schema(description = "로그 메시지", example = "Cage door opened")
		String message,

		@Schema(description = "이벤트 발생 시각")
		OffsetDateTime createdAt) {
}
