package com.peztz.backend.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.auth.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

	boolean existsByEmail(String email);

	Optional<AppUser> findByEmail(String email);
}
