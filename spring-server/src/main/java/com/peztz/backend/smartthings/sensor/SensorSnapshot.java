package com.peztz.backend.smartthings.sensor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SensorSnapshot(
		String capability,
		String attribute,
		BigDecimal numericValue,
		String stringValue,
		String unit,
		OffsetDateTime measuredAt) {
}
