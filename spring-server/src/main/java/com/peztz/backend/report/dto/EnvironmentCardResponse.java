package com.peztz.backend.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "환경 분석 카드")
public record EnvironmentCardResponse(
		String title,
		String description,
		Double averageTemperature,
		Double averageHumidity,
		long doorOpenCount,
		long lowLightCount) {
}
