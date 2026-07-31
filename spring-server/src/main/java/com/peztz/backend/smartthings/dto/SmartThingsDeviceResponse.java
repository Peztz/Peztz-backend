package com.peztz.backend.smartthings.dto;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SmartThings device response")
public record SmartThingsDeviceResponse(
		String deviceId,
		String name,
		String label,
		String manufacturerName,
		JsonNode components,
		JsonNode capabilities,
		JsonNode raw) {
}
