package com.peztz.backend.common;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API 오류 응답")
public record ApiErrorResponse(
		@Schema(description = "오류 발생 시각", example = "2026-06-08T12:00:00")
		LocalDateTime timestamp,

		@Schema(description = "HTTP 상태 코드", example = "400")
		int status,

		@Schema(description = "오류 사유", example = "Bad Request")
		String error,

		@Schema(description = "오류 메시지", example = "Invalid request")
		String message) {
}
