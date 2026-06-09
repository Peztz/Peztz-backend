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
@Tag(name = "Admin", description = "Admin read-only dashboard and management APIs")
public class AdminController {

	private final AdminService adminService;

	@Operation(summary = "Get admin dashboard summary", responses = {
			@ApiResponse(responseCode = "200", description = "Lookup succeeded",
					content = @Content(schema = @Schema(implementation = AdminSummaryResponse.class))),
			@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "Admin role required", content = @Content)
	})
	@GetMapping("/summary")
	public ResponseEntity<AdminSummaryResponse> getSummary(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getSummary(authorization));
	}

	@Operation(summary = "List facilities for admin", responses = {
			@ApiResponse(responseCode = "200", description = "Lookup succeeded",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminFacilityResponse.class)))),
			@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "Admin role required", content = @Content)
	})
	@GetMapping("/facilities")
	public ResponseEntity<List<AdminFacilityResponse>> getFacilities(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getFacilities(authorization));
	}

	@Operation(summary = "Create facility for admin", responses = {
			@ApiResponse(responseCode = "200", description = "Created",
					content = @Content(schema = @Schema(implementation = AdminFacilityResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or duplicate facility name", content = @Content),
			@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "Admin role required", content = @Content)
	})
	@PostMapping("/facilities")
	public ResponseEntity<AdminFacilityResponse> createFacility(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody AdminFacilityCreateRequest request) {
		return ResponseEntity.ok(adminService.createFacility(authorization, request));
	}

	@Operation(summary = "Update facility for admin", responses = {
			@ApiResponse(responseCode = "200", description = "Updated",
					content = @Content(schema = @Schema(implementation = AdminFacilityResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or duplicate facility name", content = @Content),
			@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "Admin role required", content = @Content),
			@ApiResponse(responseCode = "404", description = "Facility not found", content = @Content)
	})
	@PatchMapping("/facilities/{facilityId}")
	public ResponseEntity<AdminFacilityResponse> updateFacility(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "Facility ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Valid @RequestBody AdminFacilityUpdateRequest request) {
		return ResponseEntity.ok(adminService.updateFacility(authorization, facilityId, request));
	}

	@Operation(summary = "List cages for admin", responses = {
			@ApiResponse(responseCode = "200", description = "Lookup succeeded",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminCageResponse.class)))),
			@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "Admin role required", content = @Content)
	})
	@GetMapping("/cages")
	public ResponseEntity<List<AdminCageResponse>> getCages(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getCages(authorization));
	}

	@Operation(summary = "Update cage facility and device assignment", responses = {
			@ApiResponse(responseCode = "200", description = "Updated",
					content = @Content(schema = @Schema(implementation = AdminCageAssignmentResponse.class))),
			@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "Admin role required", content = @Content),
			@ApiResponse(responseCode = "404", description = "Cage, facility, or Raspberry Pi not found", content = @Content)
	})
	@PatchMapping("/cages/{cageId}/assignment")
	public ResponseEntity<AdminCageAssignmentResponse> updateCageAssignment(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "Cage ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
			@PathVariable UUID cageId,
			@RequestBody AdminCageAssignmentRequest request) {
		return ResponseEntity.ok(adminService.updateCageAssignment(authorization, cageId, request));
	}

	@Operation(summary = "List devices for admin", responses = {
			@ApiResponse(responseCode = "200", description = "Lookup succeeded",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminDeviceResponse.class)))),
			@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "Admin role required", content = @Content)
	})
	@GetMapping("/devices")
	public ResponseEntity<List<AdminDeviceResponse>> getDevices(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getDevices(authorization));
	}

	@Operation(summary = "List users for admin", responses = {
			@ApiResponse(responseCode = "200", description = "Lookup succeeded",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminUserResponse.class)))),
			@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
			@ApiResponse(responseCode = "403", description = "Admin role required", content = @Content)
	})
	@GetMapping("/users")
	public ResponseEntity<List<AdminUserResponse>> getUsers(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(adminService.getUsers(authorization));
	}
}
