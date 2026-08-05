package com.peztz.backend.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "세션 로그 생성 요청", example = """
		{
		  "type": "SENSOR",
		  "message": "온습도 센서 데이터 수신",
		  "temperature": 25.4,
		  "humidity": 61.2
		}
		""")
public record SessionLogRequest(
		@Schema(description = "로그 유형", example = "SENSOR", allowableValues = {"SENSOR", "FEED", "WATER", "MOTION", "NOTE"})
		@NotBlank
		String type,

		@Schema(description = "로그 메시지", example = "온습도 센서 데이터 수신")
		String message,

		@Schema(description = "온도", example = "25.4")
		Double temperature,

		@Schema(description = "습도", example = "61.2")
		Double humidity) {
}
