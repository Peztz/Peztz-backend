package com.peztz.backend.facility.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.facility.dto.FacilityLogResponse;
import com.peztz.backend.log.entity.SessionLog;
import com.peztz.backend.log.repository.SessionLogRepository;
import com.peztz.backend.pet.entity.Pet;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacilityLogService {

	private static final int MAX_LIMIT = 200;
	private static final Set<String> FACILITY_ROLES = Set.of(
			"FACILITY_MANAGER", "FACILITY", "HOSPITAL", "ADMIN");
	private static final Set<String> SENSOR_TYPES = Set.of(
			"DOOR_OPEN", "DOOR_CLOSED", "LOW_LIGHT", "LIGHT_RECOVERED");
	private static final Set<String> WARNING_TYPES = Set.of(
			"DOOR_OPEN", "LOW_LIGHT", "PACING", "SPINNING", "EXCESSIVE_BARKING",
			"NETWORK_OFFLINE", "TEMP_HIGH", "TEMP_LOW");

	private final AuthService authService;
	private final FacilityService facilityService;
	private final SessionLogRepository sessionLogRepository;

	@Transactional(readOnly = true)
	public List<FacilityLogResponse> findLogs(String authorization, UUID facilityId, int limit) {
		requireFacilityAccess(authorization, facilityId);
		if (limit < 1 || limit > MAX_LIMIT) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 200");
		}

		return sessionLogRepository
				.findByFacilityIdOrderByCreatedAtDesc(facilityId, PageRequest.of(0, limit))
				.stream()
				.map(this::toResponse)
				.toList();
	}

	private void requireFacilityAccess(String authorization, UUID facilityId) {
		facilityService.getFacility(facilityId);
		AppUser user = authService.requireUser(authorization);
		String role = normalize(user.getRole());

		if (!FACILITY_ROLES.contains(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Facility manager role is required");
		}
		if (!"ADMIN".equals(role)
				&& user.getHospitalId() != null
				&& !facilityId.equals(user.getHospitalId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User cannot access this facility");
		}
	}

	private FacilityLogResponse toResponse(SessionLog log) {
		AdmissionSession session = log.getSession();
		Cage cage = session.getCage();
		Pet pet = session.getPet();
		String type = normalize(log.getType());

		return new FacilityLogResponse(
				log.getId(),
				session.getId(),
				cage.getId(),
				cage.getName(),
				cage.getCageNumber(),
				pet.getId(),
				pet.getName(),
				type,
				category(log, type),
				WARNING_TYPES.contains(type) ? "WARNING" : "NORMAL",
				message(log, type),
				log.getCreatedAt());
	}

	private String category(SessionLog log, String type) {
		if (SENSOR_TYPES.contains(type) || type.startsWith("SENSOR_") || type.startsWith("TEMP_")) {
			return "SENSOR";
		}
		if (type.startsWith("ACCESS")) {
			return "ACCESS";
		}
		if (type.startsWith("SESSION")) {
			return "SESSION";
		}
		if (type.startsWith("NETWORK") || type.startsWith("DEVICE_")) {
			return "NETWORK";
		}
		if (log.getCamera() != null || Set.of("PACING", "SPINNING", "EXCESSIVE_BARKING").contains(type)) {
			return "BEHAVIOR";
		}
		return "OTHER";
	}

	private String message(SessionLog log, String type) {
		if (StringUtils.hasText(log.getMessage())) {
			return log.getMessage();
		}
		return type.isBlank() ? "이벤트가 감지되었습니다." : type + " 이벤트가 감지되었습니다.";
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}
}
