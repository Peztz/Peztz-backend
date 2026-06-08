package com.peztz.backend.admission.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.admission.dto.AccessCodeVerifyRequest;
import com.peztz.backend.admission.dto.AccessCodeVerifyResponse;
import com.peztz.backend.admission.dto.AdmissionSessionCreateRequest;
import com.peztz.backend.admission.dto.AdmissionSessionResponse;
import com.peztz.backend.admission.dto.OwnerCageResponse;
import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.repository.AdmissionSessionRepository;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.cage.repository.CageRepository;
import com.peztz.backend.cage.service.CageService;
import com.peztz.backend.cage.service.VideoUrlService;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdmissionSessionService {

	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_ENDED = "ENDED";

	private final AdmissionSessionRepository admissionSessionRepository;
	private final PetRepository petRepository;
	private final CageRepository cageRepository;
	private final AuthService authService;
	private final VideoUrlService videoUrlService;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public AdmissionSessionResponse create(String authorization, AdmissionSessionCreateRequest request) {
		AppUser owner = authService.requireUser(authorization);
		Pet pet = petRepository.findByIdAndOwnerId(request.petId(), owner.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
		Cage cage = cageRepository.findById(request.cageId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found"));

		return toResponse(createActiveSession(owner, pet, cage));
	}

	@Transactional
	public AdmissionSession createForFacility(AppUser owner, Pet pet, Cage cage) {
		return createActiveSession(owner, pet, cage);
	}

	@Transactional(readOnly = true)
	public AdmissionSessionResponse findById(String authorization, Long sessionId) {
		AppUser owner = authService.requireUser(authorization);
		return toResponse(findOwnedSession(sessionId, owner.getId()));
	}

	@Transactional(readOnly = true)
	public List<AdmissionSessionResponse> findMine(String authorization) {
		AppUser owner = authService.requireUser(authorization);
		return admissionSessionRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public AdmissionSessionResponse end(String authorization, Long sessionId) {
		AppUser owner = authService.requireUser(authorization);
		AdmissionSession session = findOwnedSession(sessionId, owner.getId());
		if (!STATUS_ACTIVE.equals(session.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admission session is not active");
		}

		session.setStatus(STATUS_ENDED);
		session.getCage().setStatus(CageService.STATUS_AVAILABLE);
		session.getCage().setUser(null);
		session.getCage().setCurrentPet(null);
		session.getCage().setAccessCode(null);
		return toResponse(session);
	}

	@Transactional(readOnly = true)
	public AccessCodeVerifyResponse verifyAccessCode(AccessCodeVerifyRequest request) {
		AdmissionSession session = admissionSessionRepository.findByAccessCode(request.accessCode())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Access code not found"));

		if (!STATUS_ACTIVE.equals(session.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admission session is not active");
		}

		return new AccessCodeVerifyResponse(
				true,
				session.getId(),
				session.getPet().getName(),
				getCageName(session.getCage()),
				videoUrlService.buildVideoUrl(session.getCage().getRaspberryPiDeviceId()));
	}

	@Transactional(readOnly = true)
	public List<OwnerCageResponse> findMyActiveCages(String authorization) {
		AppUser owner = authService.requireUser(authorization);
		return admissionSessionRepository.findByOwnerIdAndStatusOrderByCreatedAtDesc(owner.getId(), STATUS_ACTIVE).stream()
				.map(session -> new OwnerCageResponse(
						session.getId(),
						session.getPet().getId(),
						session.getPet().getName(),
						session.getCage().getId(),
						getCageName(session.getCage()),
						session.getCage().getFacility() == null ? null : session.getCage().getFacility().getName(),
						session.getCage().getStatus(),
						videoUrlService.buildVideoUrl(session.getCage().getRaspberryPiDeviceId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public AdmissionSession getOwnedSession(String authorization, Long sessionId) {
		AppUser owner = authService.requireUser(authorization);
		return findOwnedSession(sessionId, owner.getId());
	}

	@Transactional(readOnly = true)
	public AdmissionSession getSession(Long sessionId) {
		return admissionSessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission session not found"));
	}

	public AdmissionSessionResponse toResponse(AdmissionSession session) {
		return new AdmissionSessionResponse(
				session.getId(),
				session.getPet().getId(),
				session.getCage().getId(),
				session.getAccessCode(),
				session.getStatus(),
				session.getStartedAt(),
				null,
				videoUrlService.buildVideoUrl(session.getCage().getRaspberryPiDeviceId()));
	}

	private AdmissionSession createActiveSession(AppUser owner, Pet pet, Cage cage) {
		if (!CageService.STATUS_AVAILABLE.equals(cage.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cage is not available");
		}
		if (admissionSessionRepository.existsByPetIdAndStatus(pet.getId(), STATUS_ACTIVE)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pet already has an active admission session");
		}
		if (admissionSessionRepository.existsByCageIdAndStatus(cage.getId(), STATUS_ACTIVE)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cage already has an active admission session");
		}

		cage.setStatus(CageService.STATUS_OCCUPIED);
		String accessCode = generateAccessCode();
		cage.setUser(owner);
		cage.setCurrentPet(pet);
		cage.setAccessCode(accessCode);
		AdmissionSession session = AdmissionSession.builder()
				.owner(owner)
				.pet(pet)
				.cage(cage)
				.accessCode(accessCode)
				.status(STATUS_ACTIVE)
				.createdAt(OffsetDateTime.now().withNano(0))
				.build();

		return admissionSessionRepository.save(session);
	}

	private AdmissionSession findOwnedSession(Long sessionId, UUID ownerId) {
		return admissionSessionRepository.findByIdAndOwnerId(sessionId, ownerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission session not found"));
	}

	private String generateAccessCode() {
		for (int i = 0; i < 10; i++) {
			String code = String.format("%06d", secureRandom.nextInt(1_000_000));
			if (!admissionSessionRepository.existsByAccessCodeAndStatus(code, STATUS_ACTIVE)) {
				return code;
			}
		}
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate access code");
	}

	private String getCageName(Cage cage) {
		return cage.getName() == null ? "Cage " + cage.getId() : cage.getName();
	}
}
