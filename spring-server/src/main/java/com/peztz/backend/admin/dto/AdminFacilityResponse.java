package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin facility list item")
public record AdminFacilityResponse(
		UUID facilityId,
		String facilityName,
		String phoneNumber,
		String type,
		long cageCount,
		long activeSessionCount,
		long deviceIssueCount,
		String status) {
}
