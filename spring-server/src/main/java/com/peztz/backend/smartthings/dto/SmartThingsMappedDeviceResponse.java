package com.peztz.backend.smartthings.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peztz.backend.smartthings.device.SmartThingsDeviceType;

public record SmartThingsMappedDeviceResponse(
		UUID mappingId,
		UUID cageId,
		String deviceId,
		SmartThingsDeviceType deviceType,
		String label,
		Integer battery,
		boolean online,
		boolean active,
		OffsetDateTime lastSeenAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
