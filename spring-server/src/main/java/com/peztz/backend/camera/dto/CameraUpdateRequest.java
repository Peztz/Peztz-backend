package com.peztz.backend.camera.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "카메라 수정 요청입니다. 카메라는 기존 케이지에 계속 할당됩니다.")
public record CameraUpdateRequest(
		@NotBlank
		@Size(max = 100)
		@Schema(description = "카메라 표시 이름", example = "Living room camera")
		String name,

		@Size(max = 255)
		@Schema(description = "API 응답 외부에 저장된 RTSP 설정 참조 키")
		String rtspConfigKey) {
}
