package com.peztz.backend.camera.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.camera.entity.Camera;

public interface CameraRepository extends JpaRepository<Camera, UUID> {

	List<Camera> findByCageUserIdOrderByCreatedAtDesc(UUID ownerId);

	Optional<Camera> findByIdAndCageUserId(UUID id, UUID ownerId);

	boolean existsByCageId(UUID cageId);
}
