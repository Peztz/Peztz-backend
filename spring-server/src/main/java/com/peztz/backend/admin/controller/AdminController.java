package com.peztz.backend.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.admin.dto.AdminCageAssignmentRequest;
import com.peztz.backend.admin.dto.AdminCageAssignmentResponse;
import com.peztz.backend.admin.dto.AdminCageResponse;
import com.peztz.backend.admin.dto.AdminDeviceResponse;
import com.peztz.backend.admin.dto.AdminFacilityCreateRequest;
import com.peztz.backend.admin.dto.AdminFacilityResponse;
import com.peztz.backend.admin.dto.AdminFacilityUpdateRequest;
import com.peztz.backend.admin.dto.AdminSummaryResponse;
import com.peztz.backend.admin.dto.AdminUserResponse;
import com.peztz.backend.admin.service.AdminService;

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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "관리자", description = "관리자 대시보드 조회 및 관리 API")
public class AdminController {

	private final AdminService adminService;

	@Operation(summary = "관리자 대시보드 요약 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(schema = @Schema(implementation = AdminSummaryResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
	})
	@GetMapping("/summary")
	public ResponseEntity<AdminSummaryResponse> getSummary(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getSummary(authorization));
	}

	@Operation(summary = "관리자용 시설 목록 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminFacilityResponse.class)))),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
	})
	@GetMapping("/facilities")
	public ResponseEntity<List<AdminFacilityResponse>> getFacilities(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getFacilities(authorization));
	}

	@Operation(summary = "관리자용 시설 생성", responses = {
			@ApiResponse(responseCode = "200", description = "생성 성공",
					content = @Content(schema = @Schema(implementation = AdminFacilityResponse.class))),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 또는 중복된 시설명", content = @Content),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
	})
	@PostMapping("/facilities")
	public ResponseEntity<AdminFacilityResponse> createFacility(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody AdminFacilityCreateRequest request) {
		return ResponseEntity.ok(adminService.createFacility(authorization, request));
	}

	@Operation(summary = "관리자용 시설 수정", responses = {
			@ApiResponse(responseCode = "200", description = "수정 성공",
					content = @Content(schema = @Schema(implementation = AdminFacilityResponse.class))),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 또는 중복된 시설명", content = @Content),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content),
			@ApiResponse(responseCode = "404", description = "시설을 찾을 수 없음", content = @Content)
	})
	@PatchMapping("/facilities/{facilityId}")
	public ResponseEntity<AdminFacilityResponse> updateFacility(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "시설 ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Valid @RequestBody AdminFacilityUpdateRequest request) {
		return ResponseEntity.ok(adminService.updateFacility(authorization, facilityId, request));
	}

	@Operation(summary = "관리자용 케이지 목록 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminCageResponse.class)))),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
	})
	@GetMapping("/cages")
	public ResponseEntity<List<AdminCageResponse>> getCages(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getCages(authorization));
	}

	@Operation(summary = "케이지의 시설 및 장치 할당 수정", responses = {
			@ApiResponse(responseCode = "200", description = "수정 성공",
					content = @Content(schema = @Schema(implementation = AdminCageAssignmentResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content),
			@ApiResponse(responseCode = "404", description = "케이지, 시설 또는 라즈베리파이를 찾을 수 없음", content = @Content)
	})
	@PatchMapping("/cages/{cageId}/assignment")
	public ResponseEntity<AdminCageAssignmentResponse> updateCageAssignment(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "케이지 ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
			@PathVariable UUID cageId,
			@RequestBody AdminCageAssignmentRequest request) {
		return ResponseEntity.ok(adminService.updateCageAssignment(authorization, cageId, request));
	}

	@Operation(summary = "관리자용 장치 목록 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminDeviceResponse.class)))),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
	})
	@GetMapping("/devices")
	public ResponseEntity<List<AdminDeviceResponse>> getDevices(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getDevices(authorization));
	}

	@Operation(summary = "관리자용 사용자 목록 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminUserResponse.class)))),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
	})
	@GetMapping("/users")
	public ResponseEntity<List<AdminUserResponse>> getUsers(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getUsers(authorization));
	}
}
