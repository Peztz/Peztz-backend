package com.peztz.backend.facility.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.facility.dto.FacilityLogResponse;
import com.peztz.backend.facility.service.FacilityLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/facilities/{facilityId}/logs")
@RequiredArgsConstructor
@Tag(name = "시설 운영 로그", description = "시설 담당자와 관리자의 시설별 운영 로그 조회 API")
public class FacilityLogController {

	private final FacilityLogService facilityLogService;

	@Operation(
			summary = "시설 운영 로그 조회",
			description = "해당 시설의 센서 및 행동 이벤트 로그를 최신순으로 조회합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(array = @ArraySchema(schema = @Schema(implementation = FacilityLogResponse.class)))),
					@ApiResponse(responseCode = "400", description = "잘못된 조회 개수", content = @Content),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
					@ApiResponse(responseCode = "403", description = "시설 접근 권한 없음", content = @Content),
					@ApiResponse(responseCode = "404", description = "시설을 찾을 수 없음", content = @Content)
			})
	@GetMapping
	public ResponseEntity<List<FacilityLogResponse>> findLogs(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "시설 ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Parameter(description = "조회할 최대 로그 수(1~200)", example = "100")
			@RequestParam(defaultValue = "100") int limit) {
		return ResponseEntity.ok(facilityLogService.findLogs(authorization, facilityId, limit));
	}
}
