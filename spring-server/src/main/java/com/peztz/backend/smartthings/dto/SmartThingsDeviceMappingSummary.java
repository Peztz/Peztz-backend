package com.peztz.backend.smartthings.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peztz.backend.smartthings.device.SmartThingsDeviceType;

public record SmartThingsDeviceMappingSummary(
		UUID mappingId,
		UUID cageId,
		String cageName,
		SmartThingsDeviceType deviceType,
		String label,
		Integer battery,
		boolean online,
		OffsetDateTime lastSeenAt) {
}
