package com.peztz.backend.smartthings.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.smartthings.device.SmartThingsDeviceService;
import com.peztz.backend.smartthings.dto.CageSensorLatestResponse;
import com.peztz.backend.smartthings.dto.SensorReadingResponse;
import com.peztz.backend.smartthings.dto.SmartThingsDeviceRegistrationRequest;
import com.peztz.backend.smartthings.dto.SmartThingsMappedDeviceResponse;
import com.peztz.backend.smartthings.dto.SmartThingsSyncResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/smartthings")
@RequiredArgsConstructor
@Tag(name = "SmartThings cage sensors", description = "SmartThings sensor-to-cage mapping and readings")
public class SmartThingsSensorController {

	private final SmartThingsDeviceService deviceService;

	@Operation(summary = "Link a SmartThings sensor to a cage")
	@PostMapping("/cages/{cageId}/devices")
	public ResponseEntity<SmartThingsMappedDeviceResponse> register(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cageId,
			@Valid @RequestBody SmartThingsDeviceRegistrationRequest request) {
		return ResponseEntity.ok(deviceService.register(authorization, cageId, request));
	}

	@Operation(summary = "List SmartThings sensors linked to a cage")
	@GetMapping("/cages/{cageId}/devices")
	public ResponseEntity<List<SmartThingsMappedDeviceResponse>> findDevices(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cageId) {
		return ResponseEntity.ok(deviceService.findByCage(authorization, cageId));
	}

	@Operation(summary = "Immediately fetch and store one SmartThings sensor status")
	@PostMapping("/devices/{deviceId}/sync")
	public ResponseEntity<SmartThingsSyncResponse> sync(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable String deviceId) {
		return ResponseEntity.ok(deviceService.sync(authorization, deviceId));
	}

	@Operation(summary = "Get the latest reading for every sensor attribute in a cage")
	@GetMapping("/cages/{cageId}/readings/latest")
	public ResponseEntity<CageSensorLatestResponse> findLatest(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cageId) {
		return ResponseEntity.ok(deviceService.findLatest(authorization, cageId));
	}

	@Operation(summary = "Get SmartThings sensor reading history for a cage")
	@GetMapping("/cages/{cageId}/readings")
	public ResponseEntity<List<SensorReadingResponse>> findReadings(
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
