package com.peztz.backend.event.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "FastAPI 이상행동 이벤트 요청")
public record PetEventCreateRequest(
		@NotBlank @Size(max = 100)
		String externalEventId,

		@NotNull
		UUID petId,

		@NotNull
		UUID cameraId,

		@NotBlank @Size(max = 50)
		String eventType,

		@NotNull @DecimalMin("0.0") @DecimalMax("1.0")
		Double confidence,

		@NotNull
		OffsetDateTime occurredAt,

		OffsetDateTime eventEndedAt,

		@PositiveOrZero
		Integer eventDurationSeconds,

		OffsetDateTime clipStartAt,

		OffsetDateTime clipEndAt,

		@PositiveOrZero
		Integer clipDurationSeconds,

		@Size(max = 2048)
		String videoUrl,

		@Size(max = 2048)
		String thumbnailUrl,

		Map<String, Object> metadata) {
}
