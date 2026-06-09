package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin user list item")
public record AdminUserResponse(
		UUID userId,
		String name,
		String email,
		String role,
		UUID facilityId,
		String facilityName,
		String status) {
}
