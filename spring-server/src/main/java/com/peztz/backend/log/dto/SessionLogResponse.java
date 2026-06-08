package com.peztz.backend.log.dto;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Session log response", example = """
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
		@Schema(description = "Log ID", example = "90008")
		Long id,

		@Schema(description = "Log type", example = "SENSOR")
		String type,

		@Schema(description = "Log message", example = "온습도 센서 데이터 수신")
		String message,

		@Schema(description = "Temperature", example = "25.4")
		Double temperature,

		@Schema(description = "Humidity", example = "61.2")
		Double humidity,

		@Schema(description = "Created date time", example = "2026-06-08T12:30:00")
		LocalDateTime createdAt) {
}
