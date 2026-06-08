package com.peztz.backend.facility.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.facility.dto.FacilityRequest;
import com.peztz.backend.facility.dto.FacilityResponse;
import com.peztz.backend.facility.entity.Facility;
import com.peztz.backend.facility.repository.FacilityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacilityService {

	private final FacilityRepository facilityRepository;

	@Transactional(readOnly = true)
	public List<FacilityResponse> findAll() {
		return facilityRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public FacilityResponse create(FacilityRequest request) {
		Facility facility = Facility.builder()
				.name(request.name())
				.address(request.address())
				.phoneNumber(request.phoneNumber())
				.build();
		return toResponse(facilityRepository.save(facility));
	}

	@Transactional(readOnly = true)
	public FacilityResponse findById(UUID facilityId) {
		return toResponse(getFacility(facilityId));
	}

	@Transactional(readOnly = true)
	public Facility getFacility(UUID facilityId) {
		return facilityRepository.findById(facilityId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
	}

	public FacilityResponse toResponse(Facility facility) {
		return new FacilityResponse(
				facility.getId(),
				facility.getName(),
				facility.getAddress(),
				facility.getPhoneNumber(),
				null);
	}
}
