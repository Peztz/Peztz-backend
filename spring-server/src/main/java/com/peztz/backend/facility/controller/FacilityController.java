package com.peztz.backend.facility.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.facility.dto.FacilityRequest;
import com.peztz.backend.facility.dto.FacilityResponse;
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
@Tag(name = "Facility", description = "시설 등록 및 조회 API")
public class FacilityController {

	private final FacilityService facilityService;

	@Operation(summary = "시설 목록 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = FacilityResponse.class))))
	})
	@GetMapping
	public ResponseEntity<List<FacilityResponse>> findAll() {
		return ResponseEntity.ok(facilityService.findAll());
	}

	@Operation(summary = "시설 등록", responses = {
			@ApiResponse(responseCode = "200", description = "등록 성공",
					content = @Content(schema = @Schema(implementation = FacilityResponse.class))),
			@ApiResponse(responseCode = "400", description = "요청 검증 실패", content = @Content)
	})
	@PostMapping
	public ResponseEntity<FacilityResponse> create(@Valid @RequestBody FacilityRequest request) {
		return ResponseEntity.ok(facilityService.create(request));
	}

	@Operation(summary = "시설 단건 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(schema = @Schema(implementation = FacilityResponse.class))),
			@ApiResponse(responseCode = "404", description = "시설 없음", content = @Content)
	})
	@GetMapping("/{facilityId}")
	public ResponseEntity<FacilityResponse> findById(
			@Parameter(description = "Facility ID", example = "0e96bc6a-90a5-45cc-ac64-37d19254e7a2")
			@PathVariable UUID facilityId) {
		return ResponseEntity.ok(facilityService.findById(facilityId));
	}
}
