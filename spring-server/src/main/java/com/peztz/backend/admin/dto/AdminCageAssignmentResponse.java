package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 케이지 할당 수정 응답")
public record AdminCageAssignmentResponse(
		UUID cageId,
		String cageName,
		String cageNumber,
		UUID facilityId,
		String facilityName,
		UUID deviceId,
		String status) {
}
