package com.peztz.backend.event.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.event.entity.PetEvent;

public interface PetEventRepository extends JpaRepository<PetEvent, UUID> {

	Optional<PetEvent> findByExternalEventId(String externalEventId);

	List<PetEvent> findByPetOwnerIdOrderByOccurredAtDesc(UUID ownerId);

	List<PetEvent> findByPetIdAndPetOwnerIdOrderByOccurredAtDesc(UUID petId, UUID ownerId);

	Optional<PetEvent> findByIdAndPetOwnerId(UUID id, UUID ownerId);
}
