package com.peztz.backend.smartthings.dto;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스마트싱스 장치 응답")
public record SmartThingsDeviceResponse(
		String deviceId,
		String name,
		String label,
		String manufacturerName,
		JsonNode components,
		JsonNode capabilities,
		JsonNode raw) {
}
