package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin facility operation summary")
public record AdminFacilityOperationResponse(
		UUID facilityId,
		String facilityName,
		long cageCount,
		long activeSessionCount,
		long deviceIssueCount,
		String status) {
}
