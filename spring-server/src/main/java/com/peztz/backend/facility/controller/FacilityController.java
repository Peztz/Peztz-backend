package com.peztz.backend.facility.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.facility.dto.FacilityAdmissionSessionCreateRequest;
import com.peztz.backend.facility.dto.FacilityAdmissionSessionResponse;
import com.peztz.backend.facility.dto.FacilityOwnerPetResponse;
import com.peztz.backend.facility.dto.FacilityRequest;
import com.peztz.backend.facility.dto.FacilityResponse;
import com.peztz.backend.facility.service.FacilityAdmissionService;
import com.peztz.backend.facility.service.FacilityService;

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
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
@Tag(name = "Facility", description = "Facility registration, lookup, and facility admission APIs")
public class FacilityController {

	private final FacilityService facilityService;
	private final FacilityAdmissionService facilityAdmissionService;

	@Operation(summary = "List facilities", responses = {
			@ApiResponse(responseCode = "200", description = "Lookup succeeded",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = FacilityResponse.class))))
	})
	@GetMapping
	public ResponseEntity<List<FacilityResponse>> findAll() {
		return ResponseEntity.ok(facilityService.findAll());
	}

	@Operation(summary = "Create facility", responses = {
			@ApiResponse(responseCode = "200", description = "Created",
					content = @Content(schema = @Schema(implementation = FacilityResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
	})
	@PostMapping
	public ResponseEntity<FacilityResponse> create(@Valid @RequestBody FacilityRequest request) {
		return ResponseEntity.ok(facilityService.create(request));
	}

	@Operation(summary = "Get facility", responses = {
			@ApiResponse(responseCode = "200", description = "Lookup succeeded",
					content = @Content(schema = @Schema(implementation = FacilityResponse.class))),
			@ApiResponse(responseCode = "404", description = "Facility not found", content = @Content)
	})
	@GetMapping("/{facilityId}")
	public ResponseEntity<FacilityResponse> findById(
			@Parameter(description = "Facility ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId) {
		return ResponseEntity.ok(facilityService.findById(facilityId));
	}

	@Operation(
			summary = "Find owner pets for facility admission",
			description = "Facility staff can search an owner by email and list that owner's pets.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Lookup succeeded",
							content = @Content(array = @ArraySchema(schema = @Schema(implementation = FacilityOwnerPetResponse.class)))),
					@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
					@ApiResponse(responseCode = "403", description = "Facility manager role required", content = @Content),
					@ApiResponse(responseCode = "404", description = "Facility or owner not found", content = @Content)
			})
	@GetMapping("/{facilityId}/owners/pets")
	public ResponseEntity<List<FacilityOwnerPetResponse>> findOwnerPets(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "Facility ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Parameter(description = "Owner email", example = "test@naver.com", required = true)
			@RequestParam String email) {
		return ResponseEntity.ok(facilityAdmissionService.findOwnerPets(authorization, facilityId, email));
	}

	@Operation(
			summary = "Create facility admission session",
			description = "Facility staff admits an owner's pet into an available cage and issues an access code.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Created",
							content = @Content(schema = @Schema(implementation = FacilityAdmissionSessionResponse.class))),
					@ApiResponse(responseCode = "400", description = "Invalid owner pet, cage, or cage state", content = @Content),
					@ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content),
					@ApiResponse(responseCode = "403", description = "Facility manager role required", content = @Content),
					@ApiResponse(responseCode = "404", description = "Facility, owner, pet, or cage not found", content = @Content)
			})
	@PostMapping("/{facilityId}/admission-sessions")
	public ResponseEntity<FacilityAdmissionSessionResponse> createAdmissionSession(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "Facility ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Valid @RequestBody FacilityAdmissionSessionCreateRequest request) {
		return ResponseEntity.ok(facilityAdmissionService.createAdmissionSession(authorization, facilityId, request));
	}
}
