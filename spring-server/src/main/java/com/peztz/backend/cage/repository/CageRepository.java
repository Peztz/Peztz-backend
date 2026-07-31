package com.peztz.backend.cage.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.cage.entity.Cage;

public interface CageRepository extends JpaRepository<Cage, UUID> {

	List<Cage> findAllByOrderByIdAsc();

	List<Cage> findAllByFacilityIdOrderByIdAsc(UUID facilityId);

	boolean existsByIdAndFacilityId(UUID id, UUID facilityId);

	Optional<Cage> findByIdAndUserId(UUID id, UUID userId);
}
