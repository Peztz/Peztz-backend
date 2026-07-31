package com.peztz.backend.event.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.repository.AdmissionSessionRepository;
import com.peztz.backend.admission.service.AdmissionSessionService;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.camera.entity.Camera;
import com.peztz.backend.camera.service.CameraService;
import com.peztz.backend.event.dto.PetEventCreateRequest;
import com.peztz.backend.event.dto.PetEventResponse;
import com.peztz.backend.log.entity.SessionLog;
import com.peztz.backend.log.entity.SessionVideo;
import com.peztz.backend.log.repository.SessionLogRepository;
import com.peztz.backend.log.repository.SessionVideoRepository;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PetEventService {

	private static final String LEGACY_AI_EVENT_TYPE = "AI_EVENT";

	private final SessionLogRepository sessionLogRepository;
	private final SessionVideoRepository sessionVideoRepository;
	private final AdmissionSessionRepository admissionSessionRepository;
	private final PetRepository petRepository;
	private final CameraService cameraService;
	private final AuthService authService;

	@Transactional
	public PetEventResponse createFromFastApi(PetEventCreateRequest request) {
		String externalEventId = request.externalEventId().trim();
		String eventType = request.eventType().trim().toUpperCase(Locale.ROOT);
		return sessionLogRepository.findByExternalEventId(externalEventId)
				.map(this::toResponse)
				.orElseGet(() -> createNewEvent(request, externalEventId, eventType));
	}

	@Transactional(readOnly = true)
	public List<PetEventResponse> findMine(String authorization, UUID petId) {
		AppUser owner = authService.requireUser(authorization);
		List<SessionLog> events;
		if (petId == null) {
			events = sessionLogRepository.findCameraEventsByOwnerId(owner.getId());
		} else {
			petRepository.findByIdAndOwnerId(petId, owner.getId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
			events = sessionLogRepository.findCameraEventsByPetIdAndOwnerId(petId, owner.getId());
		}
		return events.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public PetEventResponse findMineById(String authorization, Long eventId) {
		AppUser owner = authService.requireUser(authorization);
		SessionLog event = sessionLogRepository.findCameraEventByIdAndOwnerId(eventId, owner.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet event not found"));
		return toResponse(event);
	}

	private PetEventResponse createNewEvent(
			PetEventCreateRequest request, String externalEventId, String eventType) {
		Pet pet = petRepository.findById(request.petId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
		Camera camera = cameraService.getCamera(request.cameraId());
		if (camera.getCage().getCurrentPet() == null
				|| !camera.getCage().getCurrentPet().getId().equals(pet.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pet is not currently assigned to the camera cage");
		}

		AdmissionSession session = admissionSessionRepository
				.findFirstByCageIdAndStatusOrderByCreatedAtDesc(camera.getCage().getId(), AdmissionSessionService.STATUS_ACTIVE)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active admission session for camera cage"));
		if (!session.getPet().getId().equals(pet.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Camera cage session does not belong to the pet");
		}

		SessionVideo video = createVideoIfPresent(session, request);
		OffsetDateTime eventEndedAt = resolveEndTime(
				request.occurredAt(), request.eventEndedAt(), request.eventDurationSeconds(), "Event");
		SessionLog event = SessionLog.builder()
				.session(session)
				.videoId(video == null ? null : video.getId())
				.camera(camera)
				.externalEventId(externalEventId)
				.type(eventType)
				.data(eventData(request))
				.createdAt(request.occurredAt())
				.eventEndedAt(eventEndedAt)
				.eventDurationSeconds(resolveDurationSeconds(
						request.occurredAt(), eventEndedAt, request.eventDurationSeconds(), "Event"))
				.build();
		return toResponse(sessionLogRepository.save(event));
	}

	private SessionVideo createVideoIfPresent(AdmissionSession session, PetEventCreateRequest request) {
		String videoUrl = normalizeUrl(request.videoUrl());
		String thumbnailUrl = normalizeUrl(request.thumbnailUrl());
		if (videoUrl == null && thumbnailUrl == null) {
			return null;
		}
		OffsetDateTime clipEndedAt = resolveEndTime(
				request.clipStartAt(), request.clipEndAt(), request.clipDurationSeconds(), "Clip");
		return sessionVideoRepository.save(SessionVideo.builder()
				.session(session)
				.videoPath(videoUrl)
				.thumbnailPath(thumbnailUrl)
				.startTime(request.clipStartAt())
				.endTime(clipEndedAt)
				.duration(resolveDurationSeconds(
						request.clipStartAt(), clipEndedAt, request.clipDurationSeconds(), "Clip"))
				.build());
	}

	private OffsetDateTime resolveEndTime(
			OffsetDateTime startedAt, OffsetDateTime suppliedEndedAt, Integer suppliedDuration, String subject) {
		if (suppliedEndedAt != null || startedAt == null || suppliedDuration == null) {
			return suppliedEndedAt;
		}
		try {
			return startedAt.plusSeconds(suppliedDuration);
		} catch (ArithmeticException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, subject + " end time is out of range", exception);
		}
	}

	private Integer resolveDurationSeconds(
			OffsetDateTime startedAt, OffsetDateTime endedAt, Integer suppliedDuration, String subject) {
		if (suppliedDuration != null && suppliedDuration < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, subject + " duration must not be negative");
		}
		if (startedAt == null || endedAt == null) {
			return suppliedDuration;
		}
		if (endedAt.isBefore(startedAt)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, subject + " end time must be after start time");
		}
		try {
			return Math.toIntExact(Duration.between(startedAt, endedAt).toSeconds());
		} catch (ArithmeticException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, subject + " duration is too large", exception);
		}
	}

	private Map<String, Object> eventData(PetEventCreateRequest request) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("confidence", request.confidence());
		data.put("metadata", request.metadata() == null ? new HashMap<>() : new HashMap<>(request.metadata()));
		return data;
	}

	private String normalizeUrl(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	public PetEventResponse toResponse(SessionLog event) {
		SessionVideo video = event.getVideoId() == null ? null : sessionVideoRepository.findById(event.getVideoId()).orElse(null);
		return new PetEventResponse(
				event.getId(),
				event.getExternalEventId(),
				event.getSession().getPet().getId(),
				event.getSession().getPet().getName(),
				event.getCamera().getId(),
				event.getCamera().getName(),
				eventType(event),
				numberValue(event.getData().get("confidence")),
				event.getEventEndedAt(),
				event.getEventDurationSeconds(),
				video == null ? null : video.getStartTime(),
				video == null ? null : video.getEndTime(),
				video == null ? null : video.getDuration(),
				video == null ? null : video.getVideoPath(),
				video == null ? null : video.getThumbnailPath(),
				metadataValue(event.getData().get("metadata")),
				event.getCreatedAt());
	}

	private String eventType(SessionLog event) {
		if (LEGACY_AI_EVENT_TYPE.equals(event.getType())) {
			return stringValue(event.getData().get("eventType"));
		}
		return event.getType();
	}

	private String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	private Double numberValue(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		return value == null ? null : Double.valueOf(value.toString());
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> metadataValue(Object value) {
		if (value instanceof Map<?, ?> map) {
			return new HashMap<>((Map<String, Object>) map);
		}
		return new HashMap<>();
	}
}
