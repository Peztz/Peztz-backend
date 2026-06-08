package com.peztz.backend.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Session log create request", example = """
		{
		  "type": "SENSOR",
		  "message": "온습도 센서 데이터 수신",
		  "temperature": 25.4,
		  "humidity": 61.2
		}
		""")
public record SessionLogRequest(
		@Schema(description = "Log type", example = "SENSOR", allowableValues = {"SENSOR", "FEED", "WATER", "MOTION", "NOTE"})
		@NotBlank
		String type,

		@Schema(description = "Log message", example = "온습도 센서 데이터 수신")
		String message,

		@Schema(description = "Temperature", example = "25.4")
		Double temperature,

		@Schema(description = "Humidity", example = "61.2")
		Double humidity) {
}
