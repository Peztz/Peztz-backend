package com.peztz.backend.smartthings.dto;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SmartThings device list response")
public record SmartThingsDeviceListResponse(
		List<SmartThingsDeviceResponse> items,
		JsonNode paging,
		JsonNode links,
		JsonNode raw) {
}
