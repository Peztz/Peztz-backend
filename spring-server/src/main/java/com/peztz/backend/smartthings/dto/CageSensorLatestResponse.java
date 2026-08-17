package com.peztz.backend.smartthings.dto;

import java.util.List;
import java.util.UUID;

public record CageSensorLatestResponse(
		UUID cageId,
		List<SensorReadingResponse> readings) {
}
