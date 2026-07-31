package com.peztz.backend.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InternalApiKeyService {

	private final String configuredApiKey;

	public InternalApiKeyService(@Value("${peztz.internal-api-key:}") String configuredApiKey) {
		this.configuredApiKey = configuredApiKey;
	}

	public void requireValid(String providedApiKey) {
		if (!StringUtils.hasText(configuredApiKey)) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Internal API key is not configured");
		}
		if (!StringUtils.hasText(providedApiKey)
				|| !MessageDigest.isEqual(
						configuredApiKey.getBytes(StandardCharsets.UTF_8),
						providedApiKey.getBytes(StandardCharsets.UTF_8))) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal API key");
		}
	}
}
