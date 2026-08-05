package com.peztz.backend.admin.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 대시보드 요약 응답")
public record AdminSummaryResponse(
		long totalUsers,
		long totalFacilities,
		long totalCages,
		long deviceIssueCount,
		List<AdminFacilityOperationResponse> facilityOperations,
		List<AdminRecentEventResponse> recentEvents) {
}
