package com.peztz.backend.common;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API error response")
public record ApiErrorResponse(
		@Schema(description = "Error timestamp", example = "2026-06-08T12:00:00")
		LocalDateTime timestamp,

		@Schema(description = "HTTP status code", example = "400")
		int status,

		@Schema(description = "Error reason", example = "Bad Request")
		String error,

		@Schema(description = "Error message", example = "Invalid request")
		String message) {
}
