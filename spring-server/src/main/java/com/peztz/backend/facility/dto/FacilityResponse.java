package com.peztz.backend.facility.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시설 응답", example = """
		{
		  "id": "0e96bc6a-90a5-45cc-ac64-37d19254e7a2",
		  "name": "Peztz 부산점",
		  "address": "부산광역시 해운대구 센텀중앙로 1",
		  "phoneNumber": "051-123-4567",
		  "createdAt": "2026-06-08T12:00:00"
		}
		""")
public record FacilityResponse(
		@Schema(description = "시설 ID", example = "0e96bc6a-90a5-45cc-ac64-37d19254e7a2")
		UUID id,

		@Schema(description = "시설명", example = "Peztz 부산점")
		String name,

		@Schema(description = "시설 주소", example = "부산광역시 해운대구 센텀중앙로 1")
		String address,

		@Schema(description = "시설 전화번호", example = "051-123-4567")
		String phoneNumber,

		@Schema(description = "생성 시각", example = "2026-06-08T12:00:00")
		LocalDateTime createdAt) {
}
