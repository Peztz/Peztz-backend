package com.peztz.backend.pet.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.pet.entity.Pet;

public interface PetRepository extends JpaRepository<Pet, UUID> {

	List<Pet> findByOwnerIdOrderByNameAsc(UUID ownerId);

	Optional<Pet> findByIdAndOwnerId(UUID id, UUID ownerId);
}
