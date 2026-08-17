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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/smartthings")
@RequiredArgsConstructor
@Tag(name = "스마트싱스", description = "스마트싱스 장치 조회 API")
@SecurityRequirement(name = "bearerAuth")
public class SmartThingsController {

	private final SmartThingsService smartThingsService;

	@Operation(summary = "스마트싱스 장치 목록 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(schema = @Schema(implementation = SmartThingsDeviceListResponse.class))),
			@ApiResponse(responseCode = "401", description = "PEZTZ 인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "허용되지 않은 PEZTZ 사용자 역할", content = @Content)
	})
	@GetMapping("/devices")
	public ResponseEntity<SmartThingsDeviceListResponse> findDevices(
			@Parameter(hidden = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(smartThingsService.findDevices(authorization));
	}

	@Operation(summary = "스마트싱스 장치 상태 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(schema = @Schema(implementation = SmartThingsDeviceStatusResponse.class))),
			@ApiResponse(responseCode = "401", description = "PEZTZ 인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "허용되지 않은 PEZTZ 사용자 역할", content = @Content),
			@ApiResponse(responseCode = "404", description = "스마트싱스 장치를 찾을 수 없음", content = @Content)
	})
	@GetMapping("/devices/{deviceId}/status")
	public ResponseEntity<SmartThingsDeviceStatusResponse> getDeviceStatus(
			@Parameter(hidden = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "스마트싱스 장치 ID", required = true)
			@PathVariable String deviceId) {
		return ResponseEntity.ok(smartThingsService.getDeviceStatus(authorization, deviceId));
	}
}
