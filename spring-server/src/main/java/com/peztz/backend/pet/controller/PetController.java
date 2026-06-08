package com.peztz.backend.pet.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.pet.dto.PetRequest;
import com.peztz.backend.pet.dto.PetResponse;
import com.peztz.backend.pet.service.PetService;

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
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Tag(name = "Pet", description = "내 반려동물 등록, 조회, 수정, 삭제 API")
public class PetController {

	private final PetService petService;

	@Operation(summary = "내 반려동물 등록", description = "로그인한 사용자에게 반려동물을 등록합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "등록 성공",
							content = @Content(schema = @Schema(implementation = PetResponse.class))),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
			})
	@PostMapping
	public ResponseEntity<PetResponse> create(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody PetRequest request) {
		return ResponseEntity.ok(petService.create(authorization, request));
	}

	@Operation(summary = "내 반려동물 목록 조회", description = "로그인한 사용자가 소유한 반려동물만 반환합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(array = @ArraySchema(schema = @Schema(implementation = PetResponse.class)))),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
			})
	@GetMapping("/my")
	public ResponseEntity<List<PetResponse>> findMine(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(petService.findMine(authorization));
	}

	@Operation(summary = "내 반려동물 단건 조회", description = "다른 사용자의 반려동물은 404를 반환합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(schema = @Schema(implementation = PetResponse.class))),
					@ApiResponse(responseCode = "404", description = "반려동물 없음 또는 접근 불가", content = @Content)
			})
	@GetMapping("/{petId}")
	public ResponseEntity<PetResponse> findById(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "Pet ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
			@PathVariable UUID petId) {
		return ResponseEntity.ok(petService.findMineById(authorization, petId));
	}

	@Operation(summary = "내 반려동물 수정", description = "다른 사용자의 반려동물은 404를 반환합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "수정 성공",
							content = @Content(schema = @Schema(implementation = PetResponse.class))),
					@ApiResponse(responseCode = "404", description = "반려동물 없음 또는 접근 불가", content = @Content)
			})
	@PutMapping("/{petId}")
	public ResponseEntity<PetResponse> update(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "Pet ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
			@PathVariable UUID petId,
			@Valid @RequestBody PetRequest request) {
		return ResponseEntity.ok(petService.update(authorization, petId, request));
	}

	@Operation(summary = "내 반려동물 삭제", description = "다른 사용자의 반려동물은 404를 반환합니다.",
			responses = {
					@ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content),
					@ApiResponse(responseCode = "404", description = "반려동물 없음 또는 접근 불가", content = @Content)
			})
	@DeleteMapping("/{petId}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "Pet ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
			@PathVariable UUID petId) {
		petService.delete(authorization, petId);
		return ResponseEntity.noContent().build();
	}
}
