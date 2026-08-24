package com.peztz.backend.integration.fastapi;

import java.util.List;

public record FastApiReportGenerationResponse(
		String summary,
		List<BehaviorCard> behaviorCards,
		EnvironmentCard environmentCard,
		List<String> careTips,
		String riskLevel,
		List<String> warnings,
		String disclaimer) {

	public record BehaviorCard(
			String title,
			String description,
			List<String> evidence) {
	}

	public record EnvironmentCard(
			String title,
			String description) {
	}
}
