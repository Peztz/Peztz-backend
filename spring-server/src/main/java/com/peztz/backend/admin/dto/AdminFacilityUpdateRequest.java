package com.peztz.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자용 시설 수정 요청")
public record AdminFacilityUpdateRequest(
		@Schema(description = "시설명", example = "Updated Animal Hospital")
		@NotBlank
		@Size(max = 20)
		String facilityName,

		@Schema(description = "시설 전화번호입니다. 빈 값이나 null이면 '-'를 저장합니다.", example = "051-999-9999")
		@Size(max = 20)
		String phoneNumber) {
}
