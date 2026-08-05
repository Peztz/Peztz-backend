package com.peztz.backend.admin.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자용 최근 시스템 이벤트 응답")
public record AdminRecentEventResponse(
		String type,
		String message,
		LocalDateTime occurredAt) {
}
