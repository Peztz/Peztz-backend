package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin cage assignment update request")
public record AdminCageAssignmentRequest(
		@Schema(description = "Facility ID to connect. Null keeps the current facility.")
		UUID facilityId,

		@Schema(description = "Raspberry Pi device ID to connect. Null keeps the current device.")
		UUID deviceId) {
}
