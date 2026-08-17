package com.peztz.backend.smartthings.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SmartThingsSyncResponse(
		String deviceId,
		int savedReadingCount,
		OffsetDateTime syncedAt,
		List<SensorReadingResponse> readings) {
}
