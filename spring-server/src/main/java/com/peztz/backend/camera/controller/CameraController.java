package com.peztz.backend.camera.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.camera.dto.CameraRequest;
import com.peztz.backend.camera.dto.CameraResponse;
import com.peztz.backend.camera.dto.CameraUpdateRequest;
import com.peztz.backend.camera.service.CameraService;
import com.peztz.backend.integration.fastapi.CameraRuntimeStatusResponse;
import com.peztz.backend.integration.fastapi.FastApiCameraClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cameras")
@RequiredArgsConstructor
@Tag(name = "카메라", description = "견주용 카메라 관리 API")
public class CameraController {

	private final CameraService cameraService;
	private final FastApiCameraClient fastApiCameraClient;

	@Operation(summary = "카메라 등록")
	@PostMapping
	public ResponseEntity<CameraResponse> create(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody CameraRequest request) {
		return ResponseEntity.ok(cameraService.create(authorization, request));
	}

	@Operation(summary = "내 카메라 목록 조회")
	@GetMapping("/my")
	public ResponseEntity<List<CameraResponse>> findMine(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(cameraService.findMine(authorization));
	}

	@Operation(summary = "내 카메라 단건 조회")
	@GetMapping("/{cameraId}")
	public ResponseEntity<CameraResponse> findById(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cameraId) {
		return ResponseEntity.ok(cameraService.findMineById(authorization, cameraId));
	}

	@Operation(summary = "내 카메라 수정")
	@PutMapping("/{cameraId}")
	public ResponseEntity<CameraResponse> update(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cameraId,
			@Valid @RequestBody CameraUpdateRequest request) {
		return ResponseEntity.ok(cameraService.update(authorization, cameraId, request));
	}

	@Operation(summary = "카메라 실행 상태 조회", description = "FastAPI HTTP 모드가 활성화되기 전까지 모의 상태를 반환합니다.")
	@GetMapping("/{cameraId}/runtime-status")
	public ResponseEntity<CameraRuntimeStatusResponse> getRuntimeStatus(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID cameraId) {
		cameraService.getOwnedCamera(authorization, cameraId);
		return ResponseEntity.ok(fastApiCameraClient.getStatus(cameraId));
	}
}
