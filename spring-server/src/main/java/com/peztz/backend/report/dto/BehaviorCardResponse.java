package com.peztz.backend.report.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행동 분석 카드")
public record BehaviorCardResponse(
		String title,
		String description,
		List<String> evidence) {
}
