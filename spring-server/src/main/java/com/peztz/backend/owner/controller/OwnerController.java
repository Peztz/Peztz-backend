package com.peztz.backend.owner.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.admission.dto.OwnerCageResponse;
import com.peztz.backend.admission.service.AdmissionSessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/owners/me")
@RequiredArgsConstructor
@Tag(name = "견주", description = "견주 전용 조회 API")
public class OwnerController {

	private final AdmissionSessionService admissionSessionService;

	@Operation(summary = "내 반려동물이 입실 중인 케이지 목록 조회", description = "ACTIVE 입실 세션 기준으로 조회합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(array = @ArraySchema(schema = @Schema(implementation = OwnerCageResponse.class)))),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
			})
	@GetMapping("/cages")
	public ResponseEntity<List<OwnerCageResponse>> findMyCages(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(admissionSessionService.findMyActiveCages(authorization));
	}
}
