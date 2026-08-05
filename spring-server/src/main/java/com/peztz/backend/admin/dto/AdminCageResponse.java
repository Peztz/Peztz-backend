package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자용 케이지 목록 항목")
public record AdminCageResponse(
		UUID cageId,
		String cageName,
		String cageNumber,
		UUID facilityId,
		String facilityName,
		UUID deviceId,
		String status,
		String currentPetName) {
}
