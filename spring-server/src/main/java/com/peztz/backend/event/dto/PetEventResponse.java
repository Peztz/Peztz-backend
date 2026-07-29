package com.peztz.backend.event.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pet abnormal behavior event response")
public record PetEventResponse(
		UUID eventId,
		String externalEventId,
		UUID petId,
		String petName,
		UUID cameraId,
		String cameraName,
		String eventType,
		Double confidence,
		OffsetDateTime occurredAt,
		String videoUrl,
		String thumbnailUrl,
		Map<String, Object> metadata,
		OffsetDateTime createdAt) {
}
