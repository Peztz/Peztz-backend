package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin cage list item")
public record AdminCageResponse(
		UUID cageId,
		String cageName,
		String cageNumber,
		String facilityName,
		UUID deviceId,
		String status,
		String currentPetName) {
}
