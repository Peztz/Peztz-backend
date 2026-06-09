package com.peztz.backend.admin.dto;

import java.time.LocalTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin device list item")
public record AdminDeviceResponse(
		UUID deviceId,
		String macAddress,
		String lastIp,
		LocalTime lastPing,
		UUID facilityId,
		String facilityName,
		UUID cageId,
		String cageName,
		String cageNumber,
		String status) {
}
