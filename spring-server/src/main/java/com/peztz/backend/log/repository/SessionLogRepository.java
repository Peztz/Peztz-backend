package com.peztz.backend.log.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.peztz.backend.log.entity.SessionLog;

public interface SessionLogRepository extends JpaRepository<SessionLog, Long> {

	List<SessionLog> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

	List<SessionLog> findBySessionIdAndCreatedAtBetweenOrderByCreatedAtAsc(
			Long sessionId,
			OffsetDateTime start,
			OffsetDateTime end);

	@Query("""
			select log
			from SessionLog log
			where log.session.pet.id = :petId
			  and log.createdAt between :start and :end
			order by log.createdAt asc
			""")
	List<SessionLog> findByPetIdAndCreatedAtBetween(
			@Param("petId") UUID petId,
			@Param("start") OffsetDateTime start,
			@Param("end") OffsetDateTime end);

	Optional<SessionLog> findByExternalEventId(String externalEventId);

	List<SessionLog> findByTypeAndSessionPetOwnerIdOrderByCreatedAtDesc(String type, UUID ownerId);

	List<SessionLog> findByTypeAndSessionPetIdAndSessionPetOwnerIdOrderByCreatedAtDesc(
			String type,
			UUID petId,
			UUID ownerId);

	Optional<SessionLog> findByIdAndTypeAndSessionPetOwnerId(Long id, String type, UUID ownerId);
}
