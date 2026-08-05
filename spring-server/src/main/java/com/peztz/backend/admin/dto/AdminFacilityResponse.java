package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자용 시설 목록 항목")
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
