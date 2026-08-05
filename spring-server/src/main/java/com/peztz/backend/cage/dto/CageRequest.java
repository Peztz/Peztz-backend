package com.peztz.backend.cage.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "케이지 생성 및 수정 요청", example = """
		{
		  "name": "A-1 케이지",
		  "cageNumber": "A-1",
		  "status": "AVAILABLE",
		  "raspberryPiDeviceId": "7bf2b0d2-dd67-4002-929a-d4505f6af890"
		}
		""")
public record CageRequest(
		@Schema(description = "케이지 이름", example = "A-1 케이지")
		String name,

		@Schema(description = "케이지 번호", example = "A-1")
		String cageNumber,

		@Schema(description = "케이지 상태", example = "AVAILABLE", allowableValues = {"AVAILABLE", "OCCUPIED", "MAINTENANCE"})
		@NotBlank
		String status,

		@Schema(description = "연결된 라즈베리파이 장치 ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		UUID raspberryPiDeviceId) {
}
