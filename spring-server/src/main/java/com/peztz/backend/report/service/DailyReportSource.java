package com.peztz.backend.report.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DailyReportSource(
		PetProfile pet,
		List<LogObservation> logs,
		List<SensorMeasurement> sensorMeasurements) {

	public DailyReportSource {
		logs = List.copyOf(logs);
		sensorMeasurements = List.copyOf(sensorMeasurements);
	}

	public record PetProfile(
			UUID id,
			String name,
			String breed,
			LocalDate birthDate) {
	}

	public record LogObservation(
			String type,
			OffsetDateTime occurredAt,
			Integer durationSeconds,
			Map<String, Object> data) {
	}

	public record SensorMeasurement(
			String attribute,
			double numericValue,
			String unit,
			OffsetDateTime measuredAt) {
	}
}
