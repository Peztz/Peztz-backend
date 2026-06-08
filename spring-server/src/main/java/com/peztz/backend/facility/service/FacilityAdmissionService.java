package com.peztz.backend.facility.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.service.AdmissionSessionService;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.repository.AppUserRepository;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.cage.repository.CageRepository;
import com.peztz.backend.cage.service.VideoUrlService;
import com.peztz.backend.facility.dto.FacilityAdmissionSessionCreateRequest;
import com.peztz.backend.facility.dto.FacilityAdmissionSessionResponse;
import com.peztz.backend.facility.dto.FacilityOwnerPetResponse;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacilityAdmissionService {

	private static final Set<String> FACILITY_ROLES = Set.of("FACILITY_MANAGER", "FACILITY", "HOSPITAL", "ADMIN");

	private final AuthService authService;
	private final AppUserRepository appUserRepository;
	private final PetRepository petRepository;
	private final CageRepository cageRepository;
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

	private AppUser findOwnerByEmail(String ownerEmail) {
		String normalizedEmail = ownerEmail.trim().toLowerCase(Locale.ROOT);
		return appUserRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));
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
}
