package com.peztz.backend.admin.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.admin.dto.AdminCageResponse;
import com.peztz.backend.admin.dto.AdminCageAssignmentRequest;
import com.peztz.backend.admin.dto.AdminCageAssignmentResponse;
import com.peztz.backend.admin.dto.AdminDeviceResponse;
import com.peztz.backend.admin.dto.AdminFacilityOperationResponse;
import com.peztz.backend.admin.dto.AdminFacilityResponse;
import com.peztz.backend.admin.dto.AdminRecentEventResponse;
import com.peztz.backend.admin.dto.AdminSummaryResponse;
import com.peztz.backend.admin.dto.AdminUserResponse;
import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.repository.AdmissionSessionRepository;
import com.peztz.backend.admission.service.AdmissionSessionService;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.repository.AppUserRepository;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.cage.repository.CageRepository;
import com.peztz.backend.device.entity.RaspberryPi;
import com.peztz.backend.device.repository.RaspberryPiRepository;
import com.peztz.backend.facility.entity.Facility;
import com.peztz.backend.facility.repository.FacilityRepository;
import com.peztz.backend.pet.entity.Pet;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

	private static final String ADMIN_ROLE = "ADMIN";
	private static final String UNKNOWN_COLUMN_VALUE = "-";
	private static final String DEVICE_ONLINE = "ONLINE";
	private static final String DEVICE_OFFLINE = "OFFLINE";
	private static final String RASPBERRY_PI_ACTIVE = "active";

	private final AuthService authService;
	private final AppUserRepository appUserRepository;
	private final FacilityRepository facilityRepository;
	private final CageRepository cageRepository;
	private final RaspberryPiRepository raspberryPiRepository;
	private final AdmissionSessionRepository admissionSessionRepository;

	@Transactional(readOnly = true)
	public AdminSummaryResponse getSummary(String authorization) {
		requireAdmin(authorization);

		List<AppUser> users = appUserRepository.findAll();
		List<Facility> facilities = facilityRepository.findAll();
		List<Cage> cages = cageRepository.findAll();
		List<RaspberryPi> devices = raspberryPiRepository.findAll();
		List<AdmissionSession> sessions = admissionSessionRepository.findAll();

		Map<UUID, Long> cageCountByFacilityId = countCagesByFacilityId(cages);
		Map<UUID, Long> activeSessionCountByFacilityId = countActiveSessionsByFacilityId(sessions);
		Map<UUID, Long> deviceIssueCountByFacilityId = countDeviceIssuesByFacilityId(cages, devices);

		List<AdminFacilityOperationResponse> facilityOperations = facilities.stream()
				.sorted(Comparator.comparing(Facility::getName, Comparator.nullsLast(String::compareTo)))
				.map(facility -> new AdminFacilityOperationResponse(
						facility.getId(),
						facility.getName(),
						cageCountByFacilityId.getOrDefault(facility.getId(), 0L),
						activeSessionCountByFacilityId.getOrDefault(facility.getId(), 0L),
						deviceIssueCountByFacilityId.getOrDefault(facility.getId(), 0L),
						UNKNOWN_COLUMN_VALUE))
				.toList();

		return new AdminSummaryResponse(
				users.size(),
				facilities.size(),
				cages.size(),
				devices.stream().filter(this::hasDeviceIssue).count(),
				facilityOperations,
				Collections.emptyList());
	}

	@Transactional(readOnly = true)
	public List<AdminFacilityResponse> getFacilities(String authorization) {
		requireAdmin(authorization);
		Map<UUID, Long> cageCountByFacilityId = countCagesByFacilityId(cageRepository.findAll());

		return facilityRepository.findAll().stream()
				.sorted(Comparator.comparing(Facility::getName, Comparator.nullsLast(String::compareTo)))
				.map(facility -> new AdminFacilityResponse(
						facility.getId(),
						facility.getName(),
						facility.getPhoneNumber(),
						UNKNOWN_COLUMN_VALUE,
						cageCountByFacilityId.getOrDefault(facility.getId(), 0L),
						UNKNOWN_COLUMN_VALUE))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AdminCageResponse> getCages(String authorization) {
		requireAdmin(authorization);

		return cageRepository.findAll().stream()
				.sorted(Comparator.comparing(Cage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
				.map(cage -> new AdminCageResponse(
						cage.getId(),
						getCageName(cage),
						cage.getCageNumber(),
						getFacilityId(cage),
						cage.getFacility() == null ? null : cage.getFacility().getName(),
						cage.getRaspberryPiDeviceId(),
						cage.getStatus(),
						getCurrentPetName(cage)))
				.toList();
	}

	@Transactional
	public AdminCageAssignmentResponse updateCageAssignment(
			String authorization,
			UUID cageId,
			AdminCageAssignmentRequest request) {
		requireAdmin(authorization);
		Cage cage = cageRepository.findById(cageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found"));

		if (request.facilityId() != null) {
			Facility facility = facilityRepository.findById(request.facilityId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
			cage.setFacility(facility);
		}
		if (request.deviceId() != null) {
			raspberryPiRepository.findById(request.deviceId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Raspberry Pi not found"));
			cage.setRaspberryPiDeviceId(request.deviceId());
		}

		return toAssignmentResponse(cage);
	}

	@Transactional(readOnly = true)
	public List<AdminDeviceResponse> getDevices(String authorization) {
		requireAdmin(authorization);
		Map<UUID, Cage> cageByDeviceId = findConnectedCagesByDeviceId(cageRepository.findAll());

		return raspberryPiRepository.findAll().stream()
				.sorted(Comparator.comparing(RaspberryPi::getDeviceId))
				.map(device -> {
					Cage cage = cageByDeviceId.get(device.getDeviceId());
					Facility facility = cage == null ? null : cage.getFacility();
					return new AdminDeviceResponse(
							device.getDeviceId(),
							device.getMacAddress(),
							device.getLastIp(),
							device.getLastPing(),
							facility == null ? null : facility.getId(),
							facility == null ? null : facility.getName(),
							cage == null ? null : cage.getId(),
							cage == null ? null : getCageName(cage),
							cage == null ? null : cage.getCageNumber(),
							toDeviceStatus(device));
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AdminUserResponse> getUsers(String authorization) {
		requireAdmin(authorization);
		Map<UUID, Facility> facilityById = facilityRepository.findAll().stream()
				.collect(Collectors.toMap(Facility::getId, Function.identity()));

		return appUserRepository.findAll().stream()
				.sorted(Comparator.comparing(AppUser::getEmail, Comparator.nullsLast(String::compareTo)))
				.map(user -> {
					Facility facility = user.getHospitalId() == null ? null : facilityById.get(user.getHospitalId());
					return new AdminUserResponse(
							user.getId(),
							user.getName(),
							user.getEmail(),
							user.getRole(),
							user.getHospitalId(),
							facility == null ? null : facility.getName(),
							UNKNOWN_COLUMN_VALUE);
				})
				.toList();
	}

	private void requireAdmin(String authorization) {
		AppUser user = authService.requireUser(authorization);
		String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
		if (!ADMIN_ROLE.equals(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
		}
	}

	private Map<UUID, Long> countCagesByFacilityId(List<Cage> cages) {
		Map<UUID, Long> counts = new HashMap<>();
		for (Cage cage : cages) {
			UUID facilityId = getFacilityId(cage);
			if (facilityId != null) {
				counts.merge(facilityId, 1L, Long::sum);
			}
		}
		return counts;
	}

	private Map<UUID, Long> countActiveSessionsByFacilityId(List<AdmissionSession> sessions) {
		Map<UUID, Long> counts = new HashMap<>();
		for (AdmissionSession session : sessions) {
			if (!AdmissionSessionService.STATUS_ACTIVE.equals(session.getStatus())) {
				continue;
			}
			UUID facilityId = getFacilityId(session.getCage());
			if (facilityId != null) {
				counts.merge(facilityId, 1L, Long::sum);
			}
		}
		return counts;
	}

	private Map<UUID, Long> countDeviceIssuesByFacilityId(List<Cage> cages, List<RaspberryPi> devices) {
		Set<UUID> issueDeviceIds = devices.stream()
				.filter(this::hasDeviceIssue)
				.map(RaspberryPi::getDeviceId)
				.collect(Collectors.toSet());
		Map<UUID, Set<UUID>> issueDeviceIdsByFacilityId = new HashMap<>();

		for (Cage cage : cages) {
			UUID facilityId = getFacilityId(cage);
			UUID deviceId = cage.getRaspberryPiDeviceId();
			if (facilityId == null || deviceId == null || !issueDeviceIds.contains(deviceId)) {
				continue;
			}
			issueDeviceIdsByFacilityId.computeIfAbsent(facilityId, ignored -> new HashSet<>()).add(deviceId);
		}

		Map<UUID, Long> counts = new HashMap<>();
		issueDeviceIdsByFacilityId.forEach((facilityId, deviceIds) -> counts.put(facilityId, (long) deviceIds.size()));
		return counts;
	}

	private Map<UUID, Cage> findConnectedCagesByDeviceId(List<Cage> cages) {
		Map<UUID, Cage> cageByDeviceId = new HashMap<>();
		List<Cage> sortedCages = new ArrayList<>(cages);
		sortedCages.sort(Comparator.comparing(Cage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
		for (Cage cage : sortedCages) {
			if (cage.getRaspberryPiDeviceId() != null) {
				cageByDeviceId.putIfAbsent(cage.getRaspberryPiDeviceId(), cage);
			}
		}
		return cageByDeviceId;
	}

	private boolean hasDeviceIssue(RaspberryPi device) {
		// TODO: Revisit the offline threshold after raspberrypi stores a date-time heartbeat instead of LocalTime.
		return !StringUtils.hasText(device.getLastIp())
				|| !RASPBERRY_PI_ACTIVE.equalsIgnoreCase(nullToEmpty(device.getIsActive()));
	}

	private String toDeviceStatus(RaspberryPi device) {
		return hasDeviceIssue(device) ? DEVICE_OFFLINE : DEVICE_ONLINE;
	}

	private UUID getFacilityId(Cage cage) {
		return cage == null || cage.getFacility() == null ? null : cage.getFacility().getId();
	}

	private String getCageName(Cage cage) {
		return cage.getName() == null ? "Cage " + cage.getId() : cage.getName();
	}

	private String getCurrentPetName(Cage cage) {
		Pet currentPet = cage.getCurrentPet();
		return currentPet == null ? null : currentPet.getName();
	}

	private AdminCageAssignmentResponse toAssignmentResponse(Cage cage) {
		Facility facility = cage.getFacility();
		return new AdminCageAssignmentResponse(
				cage.getId(),
				getCageName(cage),
				cage.getCageNumber(),
				facility == null ? null : facility.getId(),
				facility == null ? null : facility.getName(),
				cage.getRaspberryPiDeviceId(),
				cage.getStatus());
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
