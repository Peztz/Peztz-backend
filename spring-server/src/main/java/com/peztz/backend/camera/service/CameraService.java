package com.peztz.backend.camera.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.cage.repository.CageRepository;
import com.peztz.backend.camera.dto.CameraRequest;
import com.peztz.backend.camera.dto.CameraResponse;
import com.peztz.backend.camera.dto.CameraUpdateRequest;
import com.peztz.backend.camera.entity.Camera;
import com.peztz.backend.camera.repository.CameraRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CameraService {

	public static final String STATUS_REGISTERED = "REGISTERED";
	public static final String STREAM_STATUS_IDLE = "IDLE";

	private final CameraRepository cameraRepository;
	private final CageRepository cageRepository;
	private final AuthService authService;

	@Transactional
	public CameraResponse create(String authorization, CameraRequest request) {
		AppUser owner = authService.requireUser(authorization);
		Cage cage = findOwnedCage(request.cageId(), owner.getId());
		if (cameraRepository.existsByCageId(cage.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A camera is already registered for this cage");
		}

		Camera camera = Camera.builder()
				.cage(cage)
				.name(request.name().trim())
				.status(STATUS_REGISTERED)
				.streamStatus(STREAM_STATUS_IDLE)
				.rtspConfigKey(normalizeNullable(request.rtspConfigKey()))
				.build();

		return toResponse(cameraRepository.save(camera));
	}

	@Transactional(readOnly = true)
	public List<CameraResponse> findMine(String authorization) {
		AppUser owner = authService.requireUser(authorization);
		return cameraRepository.findByCageUserIdOrderByCreatedAtDesc(owner.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CameraResponse findMineById(String authorization, UUID cameraId) {
		AppUser owner = authService.requireUser(authorization);
		return toResponse(findOwnedCamera(cameraId, owner.getId()));
	}

	@Transactional
	public CameraResponse update(String authorization, UUID cameraId, CameraUpdateRequest request) {
		AppUser owner = authService.requireUser(authorization);
		Camera camera = findOwnedCamera(cameraId, owner.getId());

		camera.setName(request.name().trim());
		camera.setRtspConfigKey(normalizeNullable(request.rtspConfigKey()));
		return toResponse(camera);
	}

	@Transactional(readOnly = true)
	public Camera getCamera(UUID cameraId) {
		return cameraRepository.findById(cameraId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camera not found"));
	}

	@Transactional(readOnly = true)
	public Camera getOwnedCamera(String authorization, UUID cameraId) {
		AppUser owner = authService.requireUser(authorization);
		return findOwnedCamera(cameraId, owner.getId());
	}

	private Camera findOwnedCamera(UUID cameraId, UUID ownerId) {
		return cameraRepository.findByIdAndCageUserId(cameraId, ownerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camera not found"));
	}

	private Cage findOwnedCage(UUID cageId, UUID ownerId) {
		return cageRepository.findByIdAndUserId(cageId, ownerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found"));
	}

	private String normalizeNullable(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	public CameraResponse toResponse(Camera camera) {
		return new CameraResponse(
				camera.getId(),
				camera.getCage().getId(),
				camera.getCage().getName(),
				camera.getName(),
				camera.getStatus(),
				camera.getStreamStatus(),
				StringUtils.hasText(camera.getRtspConfigKey()),
				camera.getCreatedAt(),
				camera.getUpdatedAt());
	}
}
