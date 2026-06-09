package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin cage assignment update response")
public record AdminCageAssignmentResponse(
		UUID cageId,
		String cageName,
		String cageNumber,
		UUID facilityId,
		String facilityName,
		UUID deviceId,
		String status) {
}
