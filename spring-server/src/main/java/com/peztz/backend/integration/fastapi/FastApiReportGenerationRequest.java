package com.peztz.backend.integration.fastapi;

import java.time.LocalDate;
import java.util.List;

public record FastApiReportGenerationRequest(
		LocalDate reportDate,
		String petName,
		String breed,
		LocalDate birthDate,
		Statistics statistics,
		List<Event> events) {

	public record Statistics(
			long totalLogCount,
			long sensorLogCount,
			Double averageTemperature,
			Double averageHumidity,
			long doorOpenCount,
			long lowLightCount) {
	}

	public record Event(
			String type,
			String occurredAt,
			Integer durationSeconds,
			String message,
			Double temperature,
			Double humidity,
			Double confidence) {
	}
}
