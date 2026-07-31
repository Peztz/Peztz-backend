package com.peztz.backend.smartthings.dto;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SmartThings device status response")
public record SmartThingsDeviceStatusResponse(
		String deviceId,
		JsonNode components,
		JsonNode raw) {
}
