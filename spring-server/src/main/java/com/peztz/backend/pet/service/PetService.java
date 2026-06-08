package com.peztz.backend.pet.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.pet.dto.PetRequest;
import com.peztz.backend.pet.dto.PetResponse;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PetService {

	private final PetRepository petRepository;
	private final AuthService authService;

	@Transactional
	public PetResponse create(String authorization, PetRequest request) {
		AppUser owner = authService.requireUser(authorization);
		Pet pet = Pet.builder()
				.owner(owner)
				.name(request.name())
				.species(request.species())
				.breed(request.breed())
				.gender(request.gender())
				.birthDate(request.birthDate())
				.weightKg(request.weightKg())
				.memo(request.memo())
				.build();
		return toResponse(petRepository.save(pet));
	}

	@Transactional(readOnly = true)
	public List<PetResponse> findMine(String authorization) {
		AppUser owner = authService.requireUser(authorization);
		return petRepository.findByOwnerIdOrderByNameAsc(owner.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PetResponse findMineById(String authorization, UUID petId) {
		AppUser owner = authService.requireUser(authorization);
		return toResponse(findOwnedPet(petId, owner.getId()));
	}

	@Transactional
	public PetResponse update(String authorization, UUID petId, PetRequest request) {
		AppUser owner = authService.requireUser(authorization);
		Pet pet = findOwnedPet(petId, owner.getId());
		pet.setName(request.name());
		pet.setSpecies(request.species());
		pet.setBreed(request.breed());
		pet.setGender(request.gender());
		pet.setBirthDate(request.birthDate());
		pet.setWeightKg(request.weightKg());
		pet.setMemo(request.memo());
		return toResponse(pet);
	}

	@Transactional
	public void delete(String authorization, UUID petId) {
		AppUser owner = authService.requireUser(authorization);
		Pet pet = findOwnedPet(petId, owner.getId());
		petRepository.delete(pet);
	}

	private Pet findOwnedPet(UUID petId, UUID ownerId) {
		return petRepository.findByIdAndOwnerId(petId, ownerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
	}

	public PetResponse toResponse(Pet pet) {
		return new PetResponse(
				pet.getId(),
				pet.getOwner().getId(),
				pet.getName(),
				pet.getSpecies(),
				pet.getBreed(),
				pet.getGender(),
				pet.getBirthDate(),
				pet.getWeightKg(),
				pet.getMemo(),
				null);
	}
}
