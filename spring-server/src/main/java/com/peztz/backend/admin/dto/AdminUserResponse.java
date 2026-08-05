package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자용 사용자 목록 항목")
public record AdminUserResponse(
		UUID userId,
		String name,
		String email,
		String role,
		UUID facilityId,
		String facilityName,
		String status) {
}
