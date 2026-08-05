package com.peztz.backend.report.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.report.dto.DailyReportResponse;
import com.peztz.backend.report.service.DailyReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "일일 리포트", description = "세션 로그 기반 일일 리포트 API")
public class DailyReportController {

	private final DailyReportService dailyReportService;

	@Operation(summary = "반려동물 일일 리포트 조회", description = "해당 날짜의 모든 세션 로그를 기반으로 집계합니다. 로그가 없어도 빈 요약을 반환합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(schema = @Schema(implementation = DailyReportResponse.class))),
					@ApiResponse(responseCode = "404", description = "반려동물 없음 또는 접근 불가", content = @Content)
			})
	@GetMapping("/api/reports/daily")
	public ResponseEntity<DailyReportResponse> getDailyReport(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "반려동물 ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890", required = true)
			@RequestParam UUID petId,
			@Parameter(description = "리포트 날짜", example = "2026-06-08", required = true)
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ResponseEntity.ok(dailyReportService.getByPet(authorization, petId, date));
	}

	@Operation(summary = "입실 세션 일일 리포트 조회", description = "해당 날짜의 세션 로그만 기반으로 집계합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(schema = @Schema(implementation = DailyReportResponse.class))),
					@ApiResponse(responseCode = "404", description = "세션 없음 또는 접근 불가", content = @Content)
			})
	@GetMapping("/api/admission-sessions/{sessionId}/daily-report")
	public ResponseEntity<DailyReportResponse> getSessionDailyReport(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "세션 ID", example = "1000000002")
			@PathVariable Long sessionId,
			@Parameter(description = "리포트 날짜", example = "2026-06-08", required = true)
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ResponseEntity.ok(dailyReportService.getBySession(authorization, sessionId, date));
	}
}
