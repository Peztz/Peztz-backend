package com.peztz.backend.smartthings.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.peztz.backend.smartthings.device.SmartThingsDeviceType;

public record SensorReadingResponse(
		Long readingId,
		UUID cageId,
		Long sessionId,
		String deviceId,
		SmartThingsDeviceType deviceType,
		String capability,
		String attribute,
		BigDecimal numericValue,
		String stringValue,
		String unit,
		OffsetDateTime measuredAt,
		OffsetDateTime receivedAt,
		String source) {
}
