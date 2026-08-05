package com.peztz.backend.facility.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "시설 케이지 수정 요청")
public record FacilityCageUpdateRequest(
		@Schema(description = "케이지 이름", example = "Updated Cage")
		@NotBlank
		@Size(max = 100)
		String name,

		@Schema(description = "케이지 번호", example = "B-2")
		@Size(max = 50)
		String cageNumber,

		@Schema(description = "연결된 라즈베리파이 장치 ID입니다. 빈 값이나 null이면 현재 장치를 유지합니다.",
				example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		String raspberryPiDeviceId) {
}
