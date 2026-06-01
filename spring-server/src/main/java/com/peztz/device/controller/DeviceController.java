package com.peztz.device.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.device.dto.DeviceRegisterRequest;
import com.peztz.device.dto.DeviceResponse;
import com.peztz.device.service.DeviceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

	private final DeviceService deviceService;

	@PostMapping("/register")
	public ResponseEntity<Map<String, String>> register(@Valid @RequestBody DeviceRegisterRequest request) {
		deviceService.register(request);
		return ResponseEntity.ok(Map.of("result", "success"));
	}

	@GetMapping
	public ResponseEntity<List<DeviceResponse>> findAll() {
		return ResponseEntity.ok(deviceService.findAll());
	}
}
