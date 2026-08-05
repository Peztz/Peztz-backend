package com.peztz.backend.admission.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "입실 세션 생성 요청", example = """
		{
		  "petId": "7bf2b0d2-dd67-4002-929a-d4505f6af890",
		  "cageId": "d69fc7ff-481c-4305-b81c-551955a1ce23"
		}
		""")
public record AdmissionSessionCreateRequest(
		@Schema(description = "반려동물 ID", example = "7bf2b0d2-dd67-4002-929a-d4505f6af890")
		@NotNull
		UUID petId,

		@Schema(description = "케이지 ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
		@NotNull
		UUID cageId) {
}
