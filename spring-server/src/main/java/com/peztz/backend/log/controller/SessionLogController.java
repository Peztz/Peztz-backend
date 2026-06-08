package com.peztz.backend.log.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.log.dto.SessionLogRequest;
import com.peztz.backend.log.dto.SessionLogResponse;
import com.peztz.backend.log.service.SessionLogService;

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
@RequestMapping("/api/admission-sessions/{sessionId}/logs")
@RequiredArgsConstructor
@Tag(name = "Session Log", description = "입실 세션별 로그 등록 및 조회 API")
public class SessionLogController {

	private final SessionLogService sessionLogService;

	@Operation(summary = "세션 로그 조회", description = "로그인 사용자가 소유한 세션의 로그만 조회합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(array = @ArraySchema(schema = @Schema(implementation = SessionLogResponse.class)))),
					@ApiResponse(responseCode = "404", description = "세션 없음 또는 접근 불가", content = @Content)
			})
	@GetMapping
	public ResponseEntity<List<SessionLogResponse>> findBySession(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "Session ID", example = "3457d769-298f-43d5-a06c-3bbb97bb30d5")
			@PathVariable Long sessionId) {
		return ResponseEntity.ok(sessionLogService.findBySession(authorization, sessionId));
	}

	@Operation(summary = "세션 로그 등록", description = "로그인 사용자가 소유한 세션에 로그를 등록합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "등록 성공",
							content = @Content(schema = @Schema(implementation = SessionLogResponse.class))),
					@ApiResponse(responseCode = "404", description = "세션 없음 또는 접근 불가", content = @Content)
			})
	@PostMapping
	public ResponseEntity<SessionLogResponse> create(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "Session ID", example = "3457d769-298f-43d5-a06c-3bbb97bb30d5")
			@PathVariable Long sessionId,
			@Valid @RequestBody SessionLogRequest request) {
		return ResponseEntity.ok(sessionLogService.create(authorization, sessionId, request));
	}
}
