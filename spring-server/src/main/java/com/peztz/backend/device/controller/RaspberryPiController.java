package com.peztz.backend.device.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.common.InternalApiKeyService;
import com.peztz.backend.device.dto.RaspberryPiRegisterRequest;
import com.peztz.backend.device.dto.RaspberryPiResponse;
import com.peztz.backend.device.service.RaspberryPiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/raspberrypis")
@RequiredArgsConstructor
@Tag(name = "라즈베리파이", description = "라즈베리파이 장치 등록 및 상태 관리 내부 API")
public class RaspberryPiController {

	private final RaspberryPiService raspberryPiService;
	private final InternalApiKeyService internalApiKeyService;

	@Operation(summary = "라즈베리파이 등록 또는 상태 갱신")
	@PostMapping("/register")
	public ResponseEntity<RaspberryPiResponse> register(
			@RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey,
			@Valid @RequestBody RaspberryPiRegisterRequest request) {
		internalApiKeyService.requireValid(internalApiKey);
		return ResponseEntity.ok(raspberryPiService.register(request));
	}

	@Operation(summary = "라즈베리파이 목록 조회")
	@GetMapping
	public ResponseEntity<List<RaspberryPiResponse>> findAll(
			@RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
		internalApiKeyService.requireValid(internalApiKey);
		return ResponseEntity.ok(raspberryPiService.findAll());
	}
}
