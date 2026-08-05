package com.peztz.backend.camera.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "카메라 생성 요청")
public record CameraRequest(
		@NotNull
		@Schema(description = "인증된 사용자가 소유한 케이지 ID입니다. 케이지당 카메라 하나만 등록할 수 있습니다.")
		UUID cageId,

		@NotBlank
		@Size(max = 100)
		@Schema(description = "카메라 표시 이름", example = "Living room camera")
		String name,

		@Size(max = 255)
		@Schema(description = "API 응답 외부에 저장된 RTSP 설정 참조 키")
		String rtspConfigKey) {
}
