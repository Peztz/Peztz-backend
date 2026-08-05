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
@Tag(name = "반려동물 이벤트", description = "견주용 이상행동 이벤트 조회 API")
public class PetEventController {

	private final PetEventService petEventService;

	@Operation(summary = "내 반려동물 이벤트 목록 조회", description = "소유한 반려동물 ID로 이벤트를 선택적으로 필터링합니다.")
	@GetMapping("/my")
	public ResponseEntity<List<PetEventResponse>> findMine(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam(required = false) UUID petId) {
		return ResponseEntity.ok(petEventService.findMine(authorization, petId));
	}

	@Operation(summary = "내 반려동물 이벤트 단건 조회")
	@GetMapping("/{eventId}")
	public ResponseEntity<PetEventResponse> findById(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long eventId) {
		return ResponseEntity.ok(petEventService.findMineById(authorization, eventId));
	}
}
