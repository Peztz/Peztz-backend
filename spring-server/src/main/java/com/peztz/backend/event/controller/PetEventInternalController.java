package com.peztz.backend.event.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.common.InternalApiKeyService;
import com.peztz.backend.event.dto.PetEventCreateRequest;
import com.peztz.backend.event.dto.PetEventResponse;
import com.peztz.backend.event.service.PetEventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal/pet-events")
@RequiredArgsConstructor
@Tag(name = "Internal Pet Event", description = "FastAPI-to-Spring event ingestion API")
public class PetEventInternalController {

	private final PetEventService petEventService;
	private final InternalApiKeyService internalApiKeyService;

	@Operation(summary = "Store FastAPI abnormal behavior event")
	@PostMapping
	public ResponseEntity<PetEventResponse> create(
			@RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey,
			@Valid @RequestBody PetEventCreateRequest request) {
		internalApiKeyService.requireValid(internalApiKey);
		return ResponseEntity.ok(petEventService.createFromFastApi(request));
	}
}
