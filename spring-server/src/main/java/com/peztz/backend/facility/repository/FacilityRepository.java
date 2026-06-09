package com.peztz.backend.facility.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.facility.entity.Facility;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, UUID id);
}
