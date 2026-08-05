package com.peztz.backend.admin.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 케이지 할당 수정 요청")
public record AdminCageAssignmentRequest(
		@Schema(description = "연결할 시설 ID입니다. null이면 현재 시설을 유지합니다.")
		UUID facilityId,

		@Schema(description = "연결할 라즈베리파이 장치 ID입니다. null이면 현재 장치를 유지합니다.")
		UUID deviceId) {
}
