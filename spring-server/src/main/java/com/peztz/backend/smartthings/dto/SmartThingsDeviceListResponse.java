package com.peztz.backend.smartthings.dto;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스마트싱스 장치 목록 응답")
public record SmartThingsDeviceListResponse(
		List<SmartThingsDeviceResponse> items,
		JsonNode paging,
		JsonNode links,
		JsonNode raw) {
}
