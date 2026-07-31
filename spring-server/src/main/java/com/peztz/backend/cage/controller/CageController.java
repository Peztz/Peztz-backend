package com.peztz.backend.cage.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.cage.dto.CageRequest;
import com.peztz.backend.cage.dto.CageResponse;
import com.peztz.backend.cage.service.CageService;

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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Cage", description = "케이지 등록, 조회, 수정, 삭제 API")
public class CageController {

	private final CageService cageService;

	@Operation(summary = "전체 케이지 목록 조회",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(array = @ArraySchema(schema = @Schema(implementation = CageResponse.class))))
			})
	@GetMapping("/cages")
	public ResponseEntity<List<CageResponse>> findAll() {
		return ResponseEntity.ok(cageService.findAll());
	}

	@Operation(summary = "케이지 단건 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(schema = @Schema(implementation = CageResponse.class))),
			@ApiResponse(responseCode = "404", description = "케이지 없음", content = @Content)
	})
	@GetMapping("/cages/{cageId}")
	public ResponseEntity<CageResponse> findById(
			@Parameter(description = "Cage ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
			@PathVariable UUID cageId) {
		return ResponseEntity.ok(cageService.findById(cageId));
	}

	@Operation(summary = "시설에 케이지 등록",
			responses = {
					@ApiResponse(responseCode = "200", description = "등록 성공",
							content = @Content(schema = @Schema(implementation = CageResponse.class))),
					@ApiResponse(responseCode = "404", description = "시설 없음", content = @Content)
			})
	@PostMapping("/facilities/{facilityId}/cages")
	public ResponseEntity<CageResponse> create(
			@Parameter(description = "Facility ID", example = "0e96bc6a-90a5-45cc-ac64-37d19254e7a2")
			@PathVariable UUID facilityId,
			@Valid @RequestBody CageRequest request) {
		return ResponseEntity.ok(cageService.create(facilityId, request));
	}

	@Operation(summary = "시설별 케이지 목록 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = CageResponse.class)))),
			@ApiResponse(responseCode = "404", description = "시설 없음", content = @Content)
	})
	@GetMapping("/facilities/{facilityId}/cages")
	public ResponseEntity<List<CageResponse>> findByFacility(
			@Parameter(description = "Facility ID", example = "0e96bc6a-90a5-45cc-ac64-37d19254e7a2")
			@PathVariable UUID facilityId) {
		return ResponseEntity.ok(cageService.findByFacility(facilityId));
	}

	@Operation(summary = "케이지 수정", responses = {
			@ApiResponse(responseCode = "200", description = "수정 성공",
					content = @Content(schema = @Schema(implementation = CageResponse.class))),
			@ApiResponse(responseCode = "404", description = "케이지 없음", content = @Content)
	})
	@PutMapping("/cages/{cageId}")
	public ResponseEntity<CageResponse> update(
			@Parameter(description = "Cage ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
			@PathVariable UUID cageId,
			@Valid @RequestBody CageRequest request) {
		return ResponseEntity.ok(cageService.update(cageId, request));
	}

	@Operation(summary = "케이지 삭제", responses = {
			@ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content),
			@ApiResponse(responseCode = "404", description = "케이지 없음", content = @Content)
	})
	@DeleteMapping("/cages/{cageId}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "Cage ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
			@PathVariable UUID cageId) {
		cageService.delete(cageId);
		return ResponseEntity.noContent().build();
	}
}
