package com.peztz.backend.admin.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin recent system event response")
public record AdminRecentEventResponse(
		String type,
		String message,
		LocalDateTime occurredAt) {
}
