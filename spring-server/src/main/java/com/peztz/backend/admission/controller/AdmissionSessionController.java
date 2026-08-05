package com.peztz.backend.admission.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.admission.dto.AccessCodeVerifyRequest;
import com.peztz.backend.admission.dto.AccessCodeVerifyResponse;
import com.peztz.backend.admission.dto.AdmissionSessionCreateRequest;
import com.peztz.backend.admission.dto.AdmissionSessionResponse;
import com.peztz.backend.admission.service.AdmissionSessionService;

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
@RequestMapping("/api/admission-sessions")
@RequiredArgsConstructor
@Tag(name = "입실 세션", description = "입실 세션 생성, 조회, 종료, 접근 코드 인증 API")
public class AdmissionSessionController {

	private final AdmissionSessionService admissionSessionService;

	@Operation(summary = "입실 세션 생성", description = "내 반려동물을 케이지에 입실시키고 6자리 접근 코드를 발급합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "생성 성공",
							content = @Content(schema = @Schema(implementation = AdmissionSessionResponse.class))),
					@ApiResponse(responseCode = "400", description = "케이지 사용 불가", content = @Content),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
					@ApiResponse(responseCode = "404", description = "반려동물 또는 케이지 없음", content = @Content)
			})
	@PostMapping
	public ResponseEntity<AdmissionSessionResponse> create(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody AdmissionSessionCreateRequest request) {
		return ResponseEntity.ok(admissionSessionService.create(authorization, request));
	}

	@Operation(summary = "입실 세션 단건 조회", description = "로그인한 사용자의 세션만 조회합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(schema = @Schema(implementation = AdmissionSessionResponse.class))),
					@ApiResponse(responseCode = "404", description = "세션 없음 또는 접근 불가", content = @Content)
			})
	@GetMapping("/{sessionId}")
	public ResponseEntity<AdmissionSessionResponse> findById(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "세션 ID", example = "1000000002")
			@PathVariable Long sessionId) {
		return ResponseEntity.ok(admissionSessionService.findById(authorization, sessionId));
	}

	@Operation(summary = "내 입실 세션 목록 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdmissionSessionResponse.class)))),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
	})
	@GetMapping("/my")
	public ResponseEntity<List<AdmissionSessionResponse>> findMine(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(admissionSessionService.findMine(authorization));
	}

	@Operation(summary = "입실 세션 종료", description = "ACTIVE 세션을 종료하고 케이지 상태를 AVAILABLE로 변경합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "종료 성공",
							content = @Content(schema = @Schema(implementation = AdmissionSessionResponse.class))),
					@ApiResponse(responseCode = "400", description = "이미 종료 또는 취소된 세션", content = @Content),
					@ApiResponse(responseCode = "404", description = "세션 없음 또는 접근 불가", content = @Content)
			})
	@PatchMapping("/{sessionId}/end")
	public ResponseEntity<AdmissionSessionResponse> end(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "세션 ID", example = "1000000002")
			@PathVariable Long sessionId) {
		return ResponseEntity.ok(admissionSessionService.end(authorization, sessionId));
	}

	@Operation(summary = "접근 코드 인증", description = "ACTIVE 상태의 세션 접근 코드만 인증 성공합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "인증 성공",
							content = @Content(schema = @Schema(implementation = AccessCodeVerifyResponse.class))),
					@ApiResponse(responseCode = "400", description = "종료 또는 비활성 세션", content = @Content),
					@ApiResponse(responseCode = "404", description = "접근 코드 없음", content = @Content)
			})
	@PostMapping("/access-code/verify")
	public ResponseEntity<AccessCodeVerifyResponse> verifyAccessCode(@Valid @RequestBody AccessCodeVerifyRequest request) {
		return ResponseEntity.ok(admissionSessionService.verifyAccessCode(request));
	}
}
