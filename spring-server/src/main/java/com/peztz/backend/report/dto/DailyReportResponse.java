package com.peztz.backend.report.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Daily report response", example = """
		{
		  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "date": "2026-06-08",
		  "totalLogCount": 12,
		  "sensorLogCount": 8,
		  "averageTemperature": 25.1,
		  "averageHumidity": 60.4,
		  "summary": "오늘은 총 12개의 기록이 등록되었습니다."
		}
		""")
public record DailyReportResponse(
		@Schema(description = "Pet ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID petId,

		@Schema(description = "Report date", example = "2026-06-08")
		LocalDate date,

		@Schema(description = "Total log count", example = "12")
		long totalLogCount,

		@Schema(description = "SENSOR log count", example = "8")
		long sensorLogCount,

		@Schema(description = "Average temperature from logs with temperature", example = "25.1", nullable = true)
		Double averageTemperature,

		@Schema(description = "Average humidity from logs with humidity", example = "60.4", nullable = true)
		Double averageHumidity,

		@Schema(description = "Korean summary", example = "오늘은 총 12개의 기록이 등록되었습니다.")
		String summary) {
}
