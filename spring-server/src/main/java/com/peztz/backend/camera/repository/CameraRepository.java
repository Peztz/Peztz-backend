package com.peztz.backend.camera.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.camera.entity.Camera;

public interface CameraRepository extends JpaRepository<Camera, UUID> {

	List<Camera> findByCageUserIdOrderByCreatedAtDesc(UUID ownerId);

	List<Camera> findByCageFacilityIdOrderByCreatedAtDesc(UUID facilityId);

	Optional<Camera> findByIdAndCageUserId(UUID id, UUID ownerId);

	Optional<Camera> findByCageId(UUID cageId);

	boolean existsByCageId(UUID cageId);
}
