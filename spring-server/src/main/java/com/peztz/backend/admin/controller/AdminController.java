package com.peztz.backend.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.admin.dto.AdminCageResponse;
import com.peztz.backend.admin.dto.AdminDeviceResponse;
import com.peztz.backend.admin.dto.AdminFacilityResponse;
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
