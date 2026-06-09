package com.peztz.backend.facility.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import com.peztz.backend.auth.repository.AppUserRepository;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.cage.dto.CageResponse;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.cage.repository.CageRepository;
import com.peztz.backend.cage.service.VideoUrlService;
import com.peztz.backend.device.repository.RaspberryPiRepository;
import com.peztz.backend.facility.dto.FacilityAdmissionSessionCreateRequest;
import com.peztz.backend.facility.dto.FacilityAdmissionSessionDetailResponse;
import com.peztz.backend.facility.dto.FacilityAdmissionSessionResponse;
import com.peztz.backend.facility.dto.FacilityCageUpdateRequest;
import com.peztz.backend.facility.dto.FacilityOwnerPetResponse;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacilityAdmissionService {

	private static final Set<String> FACILITY_ROLES = Set.of("FACILITY_MANAGER", "FACILITY", "HOSPITAL", "ADMIN");
	private static final Set<String> CAGE_UPDATE_ROLES = Set.of("FACILITY_MANAGER", "FACILITY", "HOSPITAL");

	private final AuthService authService;
	private final AppUserRepository appUserRepository;
	private final PetRepository petRepository;
	private final CageRepository cageRepository;
	private final RaspberryPiRepository raspberryPiRepository;
	private final AdmissionSessionRepository admissionSessionRepository;
	private final FacilityService facilityService;
	private final AdmissionSessionService admissionSessionService;
	private final VideoUrlService videoUrlService;

	@Transactional(readOnly = true)
	public List<FacilityOwnerPetResponse> findOwnerPets(String authorization, UUID facilityId, String ownerEmail) {
		requireFacilityManager(authorization, facilityId);
		AppUser owner = findOwnerByEmail(ownerEmail);

		return petRepository.findByOwnerIdOrderByNameAsc(owner.getId()).stream()
				.map(pet -> new FacilityOwnerPetResponse(
						pet.getId(),
						owner.getId(),
						owner.getEmail(),
						pet.getName(),
						pet.getBreed(),
						pet.getBirthDate(),
						pet.getGender()))
				.toList();
	}

	@Transactional
	public FacilityAdmissionSessionResponse createAdmissionSession(
			String authorization,
			UUID facilityId,
			FacilityAdmissionSessionCreateRequest request) {
		requireFacilityManager(authorization, facilityId);
		AppUser owner = findOwnerByEmail(request.ownerEmail());
		Pet pet = petRepository.findByIdAndOwnerId(request.petId(), owner.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found for owner"));
		Cage cage = cageRepository.findById(request.cageId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found"));

		if (cage.getFacility() == null || !facilityId.equals(cage.getFacility().getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cage does not belong to facility");
		}

		AdmissionSession session = admissionSessionService.createForFacility(owner, pet, cage);
		return toResponse(session);
	}

	@Transactional(readOnly = true)
	public List<FacilityAdmissionSessionDetailResponse> findAdmissionSessions(
			String authorization,
			UUID facilityId,
			String status) {
		requireFacilityManager(authorization, facilityId);
		String normalizedStatus = normalizeStatusOrDefault(status);
		List<AdmissionSession> sessions = admissionSessionRepository
				.findByFacilityIdAndStatusOrderByCreatedAtDesc(facilityId, normalizedStatus);

		return sessions.stream()
				.map(this::toDetailResponse)
				.toList();
	}

	@Transactional
	public CageResponse updateCage(
			String authorization,
			UUID facilityId,
			UUID cageId,
			FacilityCageUpdateRequest request) {
		requireCageUpdateManager(authorization, facilityId);
		Cage cage = cageRepository.findById(cageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found"));
		if (cage.getFacility() == null || !facilityId.equals(cage.getFacility().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cage does not belong to facility");
		}

		cage.setName(request.name().trim());
		cage.setCageNumber(StringUtils.hasText(request.cageNumber()) ? request.cageNumber().trim() : null);
		UUID deviceId = parseDeviceIdOrNull(request.raspberryPiDeviceId());
		if (deviceId != null) {
			raspberryPiRepository.findById(deviceId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Raspberry Pi not found"));
			cage.setRaspberryPiDeviceId(deviceId);
		}

		return toCageResponse(cage);
	}

	@Transactional
	public FacilityAdmissionSessionDetailResponse endAdmissionSession(
			String authorization,
			UUID facilityId,
			Long sessionId) {
		requireFacilityManager(authorization, facilityId);
		AdmissionSession session = admissionSessionRepository.findByIdAndFacilityId(sessionId, facilityId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission session not found"));

		return toDetailResponse(admissionSessionService.endForFacility(session));
	}

	private void requireFacilityManager(String authorization, UUID facilityId) {
		facilityService.getFacility(facilityId);
		AppUser user = authService.requireUser(authorization);
		String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);

		// TODO: Replace this role-string check with a real facility membership check when the role model is finalized.
		if (!FACILITY_ROLES.contains(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Facility manager role is required");
		}
		if (!"ADMIN".equals(role) && user.getHospitalId() != null && !facilityId.equals(user.getHospitalId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User cannot manage this facility");
		}
	}

	private void requireCageUpdateManager(String authorization, UUID facilityId) {
		facilityService.getFacility(facilityId);
		AppUser user = authService.requireUser(authorization);
		String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);

		// TODO: Replace this role-string check with a real facility membership check when the role model is finalized.
		if (!CAGE_UPDATE_ROLES.contains(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Facility manager role is required");
		}
		if (user.getHospitalId() != null && !facilityId.equals(user.getHospitalId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User cannot manage this facility");
		}
	}

	private AppUser findOwnerByEmail(String ownerEmail) {
		String normalizedEmail = ownerEmail.trim().toLowerCase(Locale.ROOT);
		return appUserRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));
	}

	private String normalizeStatusOrDefault(String status) {
		if (status == null || status.isBlank()) {
			return AdmissionSessionService.STATUS_ACTIVE;
		}
		return status.trim().toUpperCase(Locale.ROOT);
	}

	private FacilityAdmissionSessionResponse toResponse(AdmissionSession session) {
		Cage cage = session.getCage();
		return new FacilityAdmissionSessionResponse(
				session.getId(),
				session.getPet().getId(),
				session.getPet().getName(),
				cage.getId(),
				cage.getName() == null ? "Cage " + cage.getId() : cage.getName(),
				session.getAccessCode(),
				session.getStatus(),
				session.getStartedAt(),
				null,
				videoUrlService.buildVideoUrl(cage.getRaspberryPiDeviceId()));
	}

	private FacilityAdmissionSessionDetailResponse toDetailResponse(AdmissionSession session) {
		Cage cage = session.getCage();
		AppUser owner = session.getOwner();
		return new FacilityAdmissionSessionDetailResponse(
				session.getId(),
				session.getPet().getId(),
				session.getPet().getName(),
				owner.getId(),
				owner.getEmail(),
				cage.getId(),
				cage.getName() == null ? "Cage " + cage.getId() : cage.getName(),
				cage.getCageNumber(),
				session.getAccessCode(),
				session.getStatus(),
				session.getStartedAt(),
				session.getEndedAtAsLocalDateTime(),
				videoUrlService.buildVideoUrl(cage.getRaspberryPiDeviceId()));
	}

	private CageResponse toCageResponse(Cage cage) {
		return new CageResponse(
				cage.getId(),
				cage.getFacility() == null ? null : cage.getFacility().getId(),
				cage.getName() == null ? "Cage " + cage.getId() : cage.getName(),
				cage.getCageNumber(),
				cage.getStatus(),
				cage.getRaspberryPiDeviceId(),
				videoUrlService.buildVideoUrl(cage.getRaspberryPiDeviceId()),
				cage.getCreatedAt());
	}

	private UUID parseDeviceIdOrNull(String rawDeviceId) {
		if (!StringUtils.hasText(rawDeviceId)) {
			return null;
		}
		try {
			return UUID.fromString(rawDeviceId.trim());
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid raspberryPiDeviceId");
		}
	}
}
