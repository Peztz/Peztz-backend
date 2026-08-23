package com.peztz.backend.smartthings.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.smartthings.device.SmartThingsDeviceService;
import com.peztz.backend.smartthings.device.SmartThingsDeviceRegistrationService;
import com.peztz.backend.smartthings.dto.CageSensorLatestResponse;
import com.peztz.backend.smartthings.dto.SensorReadingResponse;
import com.peztz.backend.smartthings.dto.SmartThingsDeviceRegistrationRequest;
import com.peztz.backend.smartthings.dto.SmartThingsMappedDeviceResponse;
import com.peztz.backend.smartthings.dto.SmartThingsSyncResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/smartthings")
@RequiredArgsConstructor
@Tag(name = "스마트싱스 케이지 센서", description = "스마트싱스 센서와 케이지 연결 및 측정값 조회 API")
@SecurityRequirement(name = "bearerAuth")
public class SmartThingsSensorController {

	private final SmartThingsDeviceService deviceService;
	private final SmartThingsDeviceRegistrationService registrationService;

	@Operation(
			summary = "스마트싱스 센서를 케이지에 연결",
			description = "스마트싱스에 등록된 센서 ID를 케이지와 연결하거나 기존 연결 정보를 갱신합니다.")
	@PostMapping("/cages/{cageId}/devices")
	public ResponseEntity<SmartThingsMappedDeviceResponse> register(
			@Parameter(hidden = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cageId,
			@Valid @RequestBody SmartThingsDeviceRegistrationRequest request) {
		return ResponseEntity.ok(registrationService.register(authorization, cageId, request));
	}

	@Operation(
			summary = "케이지에 연결된 스마트싱스 센서 목록 조회",
			description = "케이지에 연결된 활성 센서와 배터리, 온라인 상태 및 마지막 확인시간을 조회합니다.")
	@GetMapping("/cages/{cageId}/devices")
	public ResponseEntity<List<SmartThingsMappedDeviceResponse>> findDevices(
			@Parameter(hidden = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cageId) {
		return ResponseEntity.ok(deviceService.findByCage(authorization, cageId));
	}

	@Operation(
			summary = "케이지와 스마트싱스 센서 연결 해제",
			description = "과거 센서 이력을 보존하면서 케이지와 센서의 연결을 비활성화합니다.")
	@DeleteMapping("/cages/{cageId}/devices/{deviceId}")
	public ResponseEntity<Void> disconnect(
			@Parameter(hidden = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cageId,
			@PathVariable String deviceId) {
		deviceService.disconnect(authorization, cageId, deviceId);
		return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "센서 상태를 즉시 동기화하고 저장",
			description = "스마트싱스에서 센서의 온라인 상태와 최신 측정값을 즉시 조회하여 저장합니다.")
	@PostMapping("/devices/{deviceId}/sync")
	public ResponseEntity<SmartThingsSyncResponse> sync(
			@Parameter(hidden = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable String deviceId) {
		return ResponseEntity.ok(deviceService.sync(authorization, deviceId));
	}

	@Operation(
			summary = "케이지 센서 최신 측정값 조회",
			description = "케이지에 연결된 센서의 속성별 최신 측정값을 조회합니다.")
	@GetMapping("/cages/{cageId}/readings/latest")
	public ResponseEntity<CageSensorLatestResponse> findLatest(
			@Parameter(hidden = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cageId) {
		return ResponseEntity.ok(deviceService.findLatest(authorization, cageId));
	}

	@Operation(
			summary = "케이지 센서 측정 이력 조회",
			description = "케이지의 센서 측정 이력을 최신순으로 조회하며 기간과 조회 개수를 지정할 수 있습니다.")
	@GetMapping("/cages/{cageId}/readings")
	public ResponseEntity<List<SensorReadingResponse>> findReadings(
			@Parameter(hidden = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cageId,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(defaultValue = "100") int limit) {
		return ResponseEntity.ok(deviceService.findReadings(authorization, cageId, from, to, limit));
	}
}
