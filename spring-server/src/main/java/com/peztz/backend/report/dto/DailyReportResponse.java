package com.peztz.backend.report.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일일 리포트 응답", example = """
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
		@Schema(description = "반려동물 ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID petId,

		@Schema(description = "리포트 날짜", example = "2026-06-08")
		LocalDate date,

		@Schema(description = "전체 로그 수", example = "12")
		long totalLogCount,

		@Schema(description = "SENSOR 로그 수", example = "8")
		long sensorLogCount,

		@Schema(description = "온도 값이 있는 로그의 평균 온도", example = "25.1", nullable = true)
		Double averageTemperature,

		@Schema(description = "습도 값이 있는 로그의 평균 습도", example = "60.4", nullable = true)
		Double averageHumidity,

		@Schema(description = "한글 요약", example = "오늘은 총 12개의 기록이 등록되었습니다.")
		String summary) {
}
