package com.peztz.backend.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.auth.entity.AuthToken;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

	Optional<AuthToken> findByToken(String token);
}
