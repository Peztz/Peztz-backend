package com.peztz.backend.device.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.device.dto.RaspberryPiRegisterRequest;
import com.peztz.backend.device.dto.RaspberryPiResponse;
import com.peztz.backend.device.dto.RaspberryPiStreamResponse;
import com.peztz.backend.device.service.RaspberryPiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/raspberrypis")
@RequiredArgsConstructor
@Tag(name = "Raspberry Pi", description = "라즈베리파이 등록, 목록 조회, 스트리밍 URL 조회 API")
public class RaspberryPiController {

	private final RaspberryPiService raspberryPiService;

	@Operation(
			summary = "라즈베리파이 등록 또는 상태 갱신",
			description = "라즈베리파이 Python 코드가 MAC 주소와 현재 IP를 서버에 등록하거나 갱신합니다.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "등록 또는 갱신 성공",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = RaspberryPiResponse.class))),
					@ApiResponse(responseCode = "400", description = "요청 본문 검증 실패", content = @Content),
					@ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
			})
	@PostMapping("/register")
	public ResponseEntity<RaspberryPiResponse> register(@Valid @RequestBody RaspberryPiRegisterRequest request) {
		return ResponseEntity.ok(raspberryPiService.register(request));
	}

	@Operation(
			summary = "라즈베리파이 목록 조회",
			description = "등록된 모든 라즈베리파이 장치 목록을 조회합니다.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "목록 조회 성공",
							content = @Content(
									mediaType = "application/json",
									array = @ArraySchema(schema = @Schema(implementation = RaspberryPiResponse.class)))),
					@ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
			})
	@GetMapping
	public ResponseEntity<List<RaspberryPiResponse>> findAll() {
		return ResponseEntity.ok(raspberryPiService.findAll());
	}

	@Operation(
			summary = "deviceId 기반 스트리밍 URL 조회",
			description = "라즈베리파이 장치 UUID로 MJPEG 스트리밍 URL을 조회합니다.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "스트리밍 URL 조회 성공",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = RaspberryPiStreamResponse.class))),
					@ApiResponse(responseCode = "400", description = "잘못된 deviceId 형식 또는 lastIp 누락", content = @Content),
					@ApiResponse(responseCode = "404", description = "라즈베리파이 장치를 찾을 수 없음", content = @Content),
					@ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
			})
	@GetMapping("/{deviceId}/stream-url")
	public ResponseEntity<RaspberryPiStreamResponse> getStreamUrl(
			@Parameter(description = "라즈베리파이 장치 UUID", example = "1b03c87c-0f82-4b26-8f23-f4b6cfd8f3a1")
			@PathVariable UUID deviceId) {
		return ResponseEntity.ok(raspberryPiService.getStreamUrl(deviceId));
	}

	@Operation(
			summary = "macAddress 기반 스트리밍 URL 조회",
			description = "라즈베리파이 MAC 주소로 MJPEG 스트리밍 URL을 조회합니다.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "스트리밍 URL 조회 성공",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = RaspberryPiStreamResponse.class))),
					@ApiResponse(responseCode = "400", description = "macAddress 누락 또는 lastIp 누락", content = @Content),
					@ApiResponse(responseCode = "404", description = "라즈베리파이 장치를 찾을 수 없음", content = @Content),
					@ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
			})
	@GetMapping("/stream-url")
	public ResponseEntity<RaspberryPiStreamResponse> getStreamUrlByMacAddress(
			@Parameter(description = "라즈베리파이 MAC 주소", example = "88:A2:9E:3D:02:BD", required = true)
			@RequestParam String macAddress) {
		return ResponseEntity.ok(raspberryPiService.getStreamUrlByMacAddress(macAddress));
	}
}
