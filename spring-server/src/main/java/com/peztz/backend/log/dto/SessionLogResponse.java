package com.peztz.backend.log.dto;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 로그 응답", example = """
		{
		  "id": 90008,
		  "type": "SENSOR",
		  "message": "온습도 센서 데이터 수신",
		  "temperature": 25.4,
		  "humidity": 61.2,
		  "createdAt": "2026-06-08T12:30:00"
		}
		""")
public record SessionLogResponse(
		@Schema(description = "로그 ID", example = "90008")
		Long id,

		@Schema(description = "로그 유형", example = "SENSOR")
		String type,

		@Schema(description = "로그 메시지", example = "온습도 센서 데이터 수신")
		String message,

		@Schema(description = "온도", example = "25.4")
		Double temperature,

		@Schema(description = "습도", example = "61.2")
		Double humidity,

		@Schema(description = "생성 시각", example = "2026-06-08T12:30:00")
		LocalDateTime createdAt) {
}
