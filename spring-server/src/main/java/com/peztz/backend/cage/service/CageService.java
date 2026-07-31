package com.peztz.backend.cage.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.cage.dto.CageRequest;
import com.peztz.backend.cage.dto.CageResponse;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.cage.repository.CageRepository;
import com.peztz.backend.facility.entity.Facility;
import com.peztz.backend.facility.service.FacilityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CageService {

	public static final String STATUS_AVAILABLE = "AVAILABLE";
	public static final String STATUS_OCCUPIED = "OCCUPIED";

	private final CageRepository cageRepository;
	private final FacilityService facilityService;

	@Transactional(readOnly = true)
	public List<CageResponse> findAll() {
		return cageRepository.findAllByOrderByIdAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CageResponse findById(UUID cageId) {
		return toResponse(getCage(cageId));
	}

	@Transactional
	public CageResponse create(UUID facilityId, CageRequest request) {
		Facility facility = facilityService.getFacility(facilityId);
		Cage cage = Cage.builder()
				.facility(facility)
				.name(request.name())
				.cageNumber(request.cageNumber())
				.status(normalizeStatus(request.status()))
				.raspberryPiDeviceId(request.raspberryPiDeviceId())
				.build();
		return toResponse(cageRepository.save(cage));
	}

	@Transactional(readOnly = true)
	public List<CageResponse> findByFacility(UUID facilityId) {
		facilityService.getFacility(facilityId);
		return cageRepository.findAllByFacilityIdOrderByIdAsc(facilityId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public CageResponse update(UUID cageId, CageRequest request) {
		Cage cage = getCage(cageId);
		cage.setName(request.name());
		cage.setCageNumber(request.cageNumber());
		cage.setStatus(normalizeStatus(request.status()));
		cage.setRaspberryPiDeviceId(request.raspberryPiDeviceId());
		return toResponse(cage);
	}

	@Transactional
	public void delete(UUID cageId) {
		cageRepository.delete(getCage(cageId));
	}

	@Transactional(readOnly = true)
	public Cage getCage(UUID cageId) {
		return cageRepository.findById(cageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found"));
	}

	public CageResponse toResponse(Cage cage) {
		return new CageResponse(
				cage.getId(),
				cage.getFacility() == null ? null : cage.getFacility().getId(),
				getCageName(cage),
				cage.getCageNumber(),
				cage.getStatus(),
				cage.getRaspberryPiDeviceId(),
				cage.getCreatedAt());
	}

	private String getCageName(Cage cage) {
		return cage.getName() == null ? "Cage " + cage.getId() : cage.getName();
	}

	private String normalizeStatus(String status) {
		return status.trim().toUpperCase(Locale.ROOT);
	}
}
