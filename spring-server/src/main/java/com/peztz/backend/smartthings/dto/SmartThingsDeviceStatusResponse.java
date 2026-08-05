package com.peztz.backend.smartthings.dto;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스마트싱스 장치 상태 응답")
public record SmartThingsDeviceStatusResponse(
		String deviceId,
		JsonNode components,
		JsonNode raw) {
}
