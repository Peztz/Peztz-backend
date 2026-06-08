package com.peztz.backend.auth.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.auth.dto.AuthLoginRequest;
import com.peztz.backend.auth.dto.AuthLoginResponse;
import com.peztz.backend.auth.dto.AuthSignupRequest;
import com.peztz.backend.auth.dto.AuthUserResponse;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.entity.AuthToken;
import com.peztz.backend.auth.repository.AppUserRepository;
import com.peztz.backend.auth.repository.AuthTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final int TOKEN_EXPIRE_DAYS = 7;
	private static final String BEARER_PREFIX = "Bearer ";

	private final AppUserRepository appUserRepository;
	private final AuthTokenRepository authTokenRepository;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Transactional
	public AuthUserResponse signup(AuthSignupRequest request) {
		String email = normalizeEmail(request.email());
		if (appUserRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
		}

		AppUser user = AppUser.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode(request.password()))
				.name(request.name())
				.phoneNumber(request.phoneNumber())
				.role(request.role().trim().toUpperCase(Locale.ROOT))
				.build();

		return toUserResponse(appUserRepository.save(user));
	}

	@Transactional
	public AuthLoginResponse login(AuthLoginRequest request) {
		AppUser user = appUserRepository.findByEmail(normalizeEmail(request.email()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}

		String tokenValue = UUID.randomUUID().toString();
		LocalDateTime now = LocalDateTime.now().withNano(0);
		AuthToken token = AuthToken.builder()
				.token(tokenValue)
				.user(user)
				.createdAt(now)
				.expiresAt(now.plusDays(TOKEN_EXPIRE_DAYS))
				.build();
		authTokenRepository.save(token);

		return new AuthLoginResponse(tokenValue, toUserResponse(user));
	}

	@Transactional(readOnly = true)
	public AppUser requireUser(String authorizationHeader) {
		String token = extractBearerToken(authorizationHeader);
		AuthToken authToken = authTokenRepository.findByToken(token)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

		if (authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
		}

		return authToken.getUser();
	}

	@Transactional(readOnly = true)
	public AuthUserResponse me(String authorizationHeader) {
		return toUserResponse(requireUser(authorizationHeader));
	}

	public AuthUserResponse toUserResponse(AppUser user) {
		return new AuthUserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
	}

	private String extractBearerToken(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header must be Bearer token");
		}

		String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
		if (!StringUtils.hasText(token)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is empty");
		}
		return token;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
