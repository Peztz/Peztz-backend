package com.peztz.backend.admission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.admission.entity.AdmissionSession;

public interface AdmissionSessionRepository extends JpaRepository<AdmissionSession, Long> {

	Optional<AdmissionSession> findByIdAndOwnerId(Long id, UUID ownerId);

	List<AdmissionSession> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	List<AdmissionSession> findByOwnerIdAndStatusOrderByCreatedAtDesc(UUID ownerId, String status);

	Optional<AdmissionSession> findByAccessCode(String accessCode);

	boolean existsByAccessCodeAndStatus(String accessCode, String status);
}
