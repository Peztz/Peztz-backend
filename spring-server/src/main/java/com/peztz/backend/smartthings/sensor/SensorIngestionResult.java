package com.peztz.backend.smartthings.sensor;

import java.time.OffsetDateTime;
import java.util.List;

import com.peztz.backend.smartthings.dto.SensorReadingResponse;

public record SensorIngestionResult(
		String deviceId,
		OffsetDateTime syncedAt,
		List<SensorReadingResponse> readings) {
}
