package com.peztz.backend.event.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.event.dto.PetEventResponse;
import com.peztz.backend.event.service.PetEventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pet-events")
@RequiredArgsConstructor
@Tag(name = "Pet Event", description = "Owner abnormal behavior event lookup APIs")
public class PetEventController {

	private final PetEventService petEventService;

	@Operation(summary = "List my pet events", description = "Optionally filters events by an owned pet ID.")
	@GetMapping("/my")
	public ResponseEntity<List<PetEventResponse>> findMine(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam(required = false) UUID petId) {
		return ResponseEntity.ok(petEventService.findMine(authorization, petId));
	}

	@Operation(summary = "Get my pet event")
	@GetMapping("/{eventId}")
	public ResponseEntity<PetEventResponse> findById(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable UUID eventId) {
		return ResponseEntity.ok(petEventService.findMineById(authorization, eventId));
	}
}
