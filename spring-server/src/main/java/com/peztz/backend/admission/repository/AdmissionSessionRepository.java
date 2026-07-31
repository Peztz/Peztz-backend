package com.peztz.backend.admission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.peztz.backend.admission.entity.AdmissionSession;

public interface AdmissionSessionRepository extends JpaRepository<AdmissionSession, Long> {

	Optional<AdmissionSession> findByIdAndOwnerId(Long id, UUID ownerId);

	List<AdmissionSession> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	List<AdmissionSession> findByOwnerIdAndStatusOrderByCreatedAtDesc(UUID ownerId, String status);

	Optional<AdmissionSession> findByAccessCode(String accessCode);

	boolean existsByAccessCodeAndStatus(String accessCode, String status);

	boolean existsByPetIdAndStatus(UUID petId, String status);

	boolean existsByCageIdAndStatus(UUID cageId, String status);

	Optional<AdmissionSession> findFirstByCageIdAndStatusOrderByCreatedAtDesc(UUID cageId, String status);

	@Query("""
			select session
			from AdmissionSession session
			where session.cage.facility.id = :facilityId
			order by session.createdAt desc
			""")
	List<AdmissionSession> findByFacilityIdOrderByCreatedAtDesc(@Param("facilityId") UUID facilityId);

	@Query("""
			select session
			from AdmissionSession session
			where session.cage.facility.id = :facilityId
			  and session.status = :status
			order by session.createdAt desc
			""")
	List<AdmissionSession> findByFacilityIdAndStatusOrderByCreatedAtDesc(
			@Param("facilityId") UUID facilityId,
			@Param("status") String status);

	@Query("""
			select session
			from AdmissionSession session
			where session.id = :sessionId
			  and session.cage.facility.id = :facilityId
			""")
	Optional<AdmissionSession> findByIdAndFacilityId(
			@Param("sessionId") Long sessionId,
			@Param("facilityId") UUID facilityId);
}
