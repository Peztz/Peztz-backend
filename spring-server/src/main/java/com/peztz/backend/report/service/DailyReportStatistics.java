package com.peztz.backend.report.service;

public record DailyReportStatistics(
		long totalLogCount,
		long sensorLogCount,
		Double averageTemperature,
		Double averageHumidity,
		long doorOpenCount,
		long lowLightCount) {
}
