package com.peztz.backend.smartthings.device;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.admission.repository.AdmissionSessionRepository;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.cage.repository.CageRepository;
import com.peztz.backend.smartthings.dto.CageSensorLatestResponse;
import com.peztz.backend.smartthings.dto.SensorReadingResponse;
import com.peztz.backend.smartthings.dto.SmartThingsDeviceRegistrationRequest;
import com.peztz.backend.smartthings.dto.SmartThingsMappedDeviceResponse;
import com.peztz.backend.smartthings.dto.SmartThingsSyncResponse;
import com.peztz.backend.smartthings.sensor.SensorIngestionResult;
import com.peztz.backend.smartthings.sensor.SensorReading;
import com.peztz.backend.smartthings.sensor.SensorReadingMapper;
import com.peztz.backend.smartthings.sensor.SensorReadingRepository;
import com.peztz.backend.smartthings.sensor.SmartThingsSensorSyncService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmartThingsDeviceService {

	private static final Set<String> MANAGER_ROLES = Set.of("ADMIN", "FACILITY_MANAGER", "FACILITY", "HOSPITAL");
	private static final String OWNER_ROLE = "OWNER";
	private static final String ACTIVE_SESSION_STATUS = "ACTIVE";

	private final SmartThingsDeviceRepository deviceRepository;
	private final SensorReadingRepository readingRepository;
	private final AdmissionSessionRepository admissionSessionRepository;
	private final CageRepository cageRepository;
	private final AuthService authService;
	private final SmartThingsSensorSyncService syncService;
	private final SensorReadingMapper readingMapper;

	@Transactional
	public SmartThingsMappedDeviceResponse registerValidated(
			String authorization,
			UUID cageId,
			SmartThingsDeviceRegistrationRequest request,
			SmartThingsDeviceType deviceType) {
		Cage cage = cageRepository.findById(cageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found"));
		requireManager(authorization, cage);
		String externalDeviceId = request.deviceId().trim();

		SmartThingsDevice device = deviceRepository.findBySmartThingsDeviceId(externalDeviceId)
				.map(existing -> updateExisting(existing, cage, deviceType, request.label()))
				.orElseGet(() -> SmartThingsDevice.builder()
						.cage(cage)
						.smartThingsDeviceId(externalDeviceId)
						.deviceType(deviceType)
						.label(normalizeLabel(request.label()))
						.online(false)
						.active(true)
						.build());
		return toResponse(deviceRepository.save(device));
	}

	@Transactional(readOnly = true)
	public void requireCageAccess(String authorization, UUID cageId) {
		requireManagerCage(authorization, cageId);
	}

	@Transactional(readOnly = true)
	public SmartThingsMappedDeviceResponse findByMappingId(String authorization, UUID mappingId) {
		SmartThingsDevice device = deviceRepository.findById(mappingId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "SmartThings device mapping not found"));
		requireManager(authorization, device.getCage());
		return toResponse(device);
	}

	@Transactional(readOnly = true)
	public List<SmartThingsMappedDeviceResponse> findByCage(String authorization, UUID cageId) {
		Cage cage = requireManagerCage(authorization, cageId);
		return deviceRepository.findByCageIdAndActiveTrueOrderByCreatedAtAsc(cage.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public void disconnect(String authorization, UUID cageId, String smartThingsDeviceId) {
		Cage cage = requireManagerCage(authorization, cageId);
		SmartThingsDevice device = deviceRepository.findBySmartThingsDeviceId(smartThingsDeviceId)
				.filter(existing -> existing.getCage().getId().equals(cage.getId()))
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Active SmartThings device mapping was not found for this cage"));
		if (!device.isActive()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Active SmartThings device mapping was not found for this cage");
		}
		device.setActive(false);
		resetRuntimeState(device);
	}

	@Transactional
	public SmartThingsSyncResponse sync(String authorization, String smartThingsDeviceId) {
		SmartThingsDevice device = deviceRepository.findBySmartThingsDeviceId(smartThingsDeviceId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SmartThings device mapping not found"));
		requireManager(authorization, device.getCage());
		if (!device.isActive()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "SmartThings device mapping is not active");
		}
		SensorIngestionResult result = syncService.sync(device.getId(), "MANUAL");
		return new SmartThingsSyncResponse(
				result.deviceId(),
				result.readings().size(),
				result.syncedAt(),
				result.readings());
	}

	@Transactional(readOnly = true)
	public CageSensorLatestResponse findLatest(String authorization, UUID cageId) {
		Cage cage = requireCageReadAccess(authorization, cageId);
		List<SensorReading> recent = readingRepository.findByCageIdAndDeviceActiveTrueOrderByMeasuredAtDesc(
				cage.getId(), PageRequest.of(0, 1000));
		Map<String, SensorReading> latest = new LinkedHashMap<>();
		for (SensorReading reading : recent) {
			String key = reading.getDevice().getId() + ":" + reading.getCapability() + ":" + reading.getAttribute();
			latest.putIfAbsent(key, reading);
		}
		return new CageSensorLatestResponse(
				cage.getId(), latest.values().stream().map(readingMapper::toResponse).toList());
	}

	@Transactional(readOnly = true)
	public List<SensorReadingResponse> findReadings(
			String authorization,
			UUID cageId,
			OffsetDateTime from,
			OffsetDateTime to,
			int limit) {
		Cage cage = requireManagerCage(authorization, cageId);
		if ((from == null) != (to == null)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to must be supplied together");
		}
		if (from != null && to.isBefore(from)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must not be before from");
		}
		int safeLimit = Math.max(1, Math.min(limit, 500));
		List<SensorReading> readings = from == null
				? readingRepository.findByCageIdOrderByMeasuredAtDesc(cage.getId(), PageRequest.of(0, safeLimit))
				: readingRepository.findByCageIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
						cage.getId(), from, to, PageRequest.of(0, safeLimit));
		return readings.stream().map(readingMapper::toResponse).toList();
	}

	private SmartThingsDevice updateExisting(
			SmartThingsDevice existing,
			Cage cage,
			SmartThingsDeviceType deviceType,
			String label) {
		boolean cageChanged = !existing.getCage().getId().equals(cage.getId());
		if (cageChanged && existing.isActive()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "SmartThings device is already linked to another cage");
		}
		if (cageChanged) {
			existing.setCage(cage);
		}
		if (!existing.isActive() || cageChanged) {
			resetRuntimeState(existing);
		}
		existing.setDeviceType(deviceType);
		existing.setLabel(normalizeLabel(label));
		existing.setActive(true);
		return existing;
	}

	private Cage requireManagerCage(String authorization, UUID cageId) {
		Cage cage = cageRepository.findById(cageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found"));
		requireManager(authorization, cage);
		return cage;
	}

	private Cage requireCageReadAccess(String authorization, UUID cageId) {
		Cage cage = cageRepository.findById(cageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found"));
		AppUser user = authService.requireUser(authorization);
		String role = normalizedRole(user);
		if (MANAGER_ROLES.contains(role)) {
			requireFacilityAccess(user, role, cage);
			return cage;
		}
		if (OWNER_ROLE.equals(role)
				&& admissionSessionRepository.existsByCageIdAndOwnerIdAndStatus(
						cageId, user.getId(), ACTIVE_SESSION_STATUS)) {
			return cage;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User cannot read this cage");
	}

	private void requireManager(String authorization, Cage cage) {
		AppUser user = authService.requireUser(authorization);
		String role = normalizedRole(user);
		if (!MANAGER_ROLES.contains(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Facility manager role is required");
		}
		requireFacilityAccess(user, role, cage);
	}

	private void requireFacilityAccess(AppUser user, String role, Cage cage) {
		if ("ADMIN".equals(role)) {
			return;
		}
		if (cage.getFacility() == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cage is not assigned to a facility");
		}
		if (user.getHospitalId() != null && !user.getHospitalId().equals(cage.getFacility().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User cannot manage this facility");
		}
	}

	private String normalizeLabel(String label) {
		return StringUtils.hasText(label) ? label.trim() : null;
	}

	private void resetRuntimeState(SmartThingsDevice device) {
		device.setBattery(null);
		device.setOnline(false);
		device.setLastSeenAt(null);
	}

	private String normalizedRole(AppUser user) {
		return user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
	}

	private SmartThingsMappedDeviceResponse toResponse(SmartThingsDevice device) {
		return new SmartThingsMappedDeviceResponse(
				device.getId(),
				device.getCage().getId(),
				device.getSmartThingsDeviceId(),
				device.getDeviceType(),
				device.getLabel(),
				device.getBattery(),
				device.isOnline(),
				device.isActive(),
				device.getLastSeenAt(),
				device.getCreatedAt(),
				device.getUpdatedAt());
	}
}
