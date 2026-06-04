package com.peztz.backend.device.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.device.dto.RaspberryPiRegisterRequest;
import com.peztz.backend.device.dto.RaspberryPiResponse;
import com.peztz.backend.device.service.RaspberryPiService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/raspberrypis")
@RequiredArgsConstructor
public class RaspberryPiController {

	private final RaspberryPiService raspberryPiService;

	@PostMapping("/register")
	public ResponseEntity<RaspberryPiResponse> register(@Valid @RequestBody RaspberryPiRegisterRequest request) {
		return ResponseEntity.ok(raspberryPiService.register(request));
	}

	@GetMapping
	public ResponseEntity<List<RaspberryPiResponse>> findAll() {
		return ResponseEntity.ok(raspberryPiService.findAll());
	}
}
