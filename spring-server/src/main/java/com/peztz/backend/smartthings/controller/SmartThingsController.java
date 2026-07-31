package com.peztz.backend.smartthings.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.smartthings.dto.SmartThingsDeviceListResponse;
import com.peztz.backend.smartthings.dto.SmartThingsDeviceStatusResponse;
import com.peztz.backend.smartthings.service.SmartThingsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/smartthings")
@RequiredArgsConstructor
@Tag(name = "SmartThings", description = "SmartThings device lookup APIs")
public class SmartThingsController {

	private final SmartThingsService smartThingsService;

	@Operation(summary = "List SmartThings devices", responses = {
			@ApiResponse(responseCode = "200", description = "Lookup succeeded",
					content = @Content(schema = @Schema(implementation = SmartThingsDeviceListResponse.class))),
			@ApiResponse(responseCode = "401", description = "PEZTZ authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "PEZTZ role is not allowed", content = @Content)
	})
	@GetMapping("/devices")
	public ResponseEntity<SmartThingsDeviceListResponse> findDevices(
			@Parameter(description = "PEZTZ Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(smartThingsService.findDevices(authorization));
	}

	@Operation(summary = "Get SmartThings device status", responses = {
			@ApiResponse(responseCode = "200", description = "Lookup succeeded",
					content = @Content(schema = @Schema(implementation = SmartThingsDeviceStatusResponse.class))),
			@ApiResponse(responseCode = "401", description = "PEZTZ authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "PEZTZ role is not allowed", content = @Content),
			@ApiResponse(responseCode = "404", description = "SmartThings device not found", content = @Content)
	})
	@GetMapping("/devices/{deviceId}/status")
	public ResponseEntity<SmartThingsDeviceStatusResponse> getDeviceStatus(
			@Parameter(description = "PEZTZ Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "SmartThings device ID", required = true)
			@PathVariable String deviceId) {
		return ResponseEntity.ok(smartThingsService.getDeviceStatus(authorization, deviceId));
	}
}
