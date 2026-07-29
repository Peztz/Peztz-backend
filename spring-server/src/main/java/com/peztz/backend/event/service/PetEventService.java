package com.peztz.backend.event.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.camera.entity.Camera;
import com.peztz.backend.camera.service.CameraService;
import com.peztz.backend.event.dto.PetEventCreateRequest;
import com.peztz.backend.event.dto.PetEventResponse;
import com.peztz.backend.event.entity.PetEvent;
import com.peztz.backend.event.repository.PetEventRepository;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PetEventService {

	private final PetEventRepository petEventRepository;
	private final PetRepository petRepository;
	private final CameraService cameraService;
	private final AuthService authService;

	@Transactional
	public PetEventResponse createFromFastApi(PetEventCreateRequest request) {
		String externalEventId = request.externalEventId().trim();
		return petEventRepository.findByExternalEventId(externalEventId)
				.map(this::toResponse)
				.orElseGet(() -> createNewEvent(request, externalEventId));
	}

	@Transactional(readOnly = true)
	public List<PetEventResponse> findMine(String authorization, UUID petId) {
		AppUser owner = authService.requireUser(authorization);
		List<PetEvent> events;
		if (petId == null) {
			events = petEventRepository.findByPetOwnerIdOrderByOccurredAtDesc(owner.getId());
		} else {
			petRepository.findByIdAndOwnerId(petId, owner.getId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
			events = petEventRepository.findByPetIdAndPetOwnerIdOrderByOccurredAtDesc(petId, owner.getId());
		}
		return events.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public PetEventResponse findMineById(String authorization, UUID eventId) {
		AppUser owner = authService.requireUser(authorization);
		PetEvent event = petEventRepository.findByIdAndPetOwnerId(eventId, owner.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet event not found"));
		return toResponse(event);
	}

	private PetEventResponse createNewEvent(PetEventCreateRequest request, String externalEventId) {
		Pet pet = petRepository.findById(request.petId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
		Camera camera = cameraService.getCamera(request.cameraId());
		if (camera.getCage().getCurrentPet() == null
				|| !camera.getCage().getCurrentPet().getId().equals(pet.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pet is not currently assigned to the camera cage");
		}

		PetEvent event = PetEvent.builder()
				.externalEventId(externalEventId)
				.pet(pet)
				.camera(camera)
				.eventType(request.eventType().trim().toUpperCase(Locale.ROOT))
				.confidence(request.confidence())
				.occurredAt(request.occurredAt())
				.videoUrl(normalizeUrl(request.videoUrl()))
				.thumbnailUrl(normalizeUrl(request.thumbnailUrl()))
				.metadata(request.metadata() == null ? new HashMap<>() : new HashMap<>(request.metadata()))
				.build();
		return toResponse(petEventRepository.save(event));
	}

	private String normalizeUrl(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	public PetEventResponse toResponse(PetEvent event) {
		return new PetEventResponse(
				event.getId(),
				event.getExternalEventId(),
				event.getPet().getId(),
				event.getPet().getName(),
				event.getCamera().getId(),
				event.getCamera().getName(),
				event.getEventType(),
				event.getConfidence(),
				event.getOccurredAt(),
				event.getVideoUrl(),
				event.getThumbnailUrl(),
				new HashMap<>(event.getMetadata()),
				event.getCreatedAt());
	}
}
