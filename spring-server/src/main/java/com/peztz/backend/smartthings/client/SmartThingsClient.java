package com.peztz.backend.smartthings.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.smartthings.config.SmartThingsProperties;
import com.peztz.backend.smartthings.exception.SmartThingsApiException;

@Component
public class SmartThingsClient {

	private static final Logger log = LoggerFactory.getLogger(SmartThingsClient.class);

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public SmartThingsClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, SmartThingsProperties properties) {
		this.restClient = restClientBuilder
				.baseUrl(properties.getBaseUrl())
				.build();
		this.objectMapper = objectMapper;
	}

	public JsonNode getDevices(String accessToken, String locationId) {
		return get("/devices", accessToken, null, locationId);
	}

	public JsonNode getDevice(String accessToken, String deviceId) {
		return get("/devices/{deviceId}", accessToken, deviceId, null);
	}

	public JsonNode getDeviceStatus(String accessToken, String deviceId) {
		return get("/devices/{deviceId}/status", accessToken, deviceId, null);
	}

	public JsonNode getDeviceHealth(String accessToken, String deviceId) {
		return get("/devices/{deviceId}/health", accessToken, deviceId, null);
	}

	private JsonNode get(String path, String accessToken, String deviceId, String locationId) {
		Instant startedAt = Instant.now();
		String endpoint = deviceId == null ? path : path.replace("{deviceId}", safeDeviceId(deviceId));
		try {
			return restClient.get()
					.uri(uriBuilder -> buildUri(uriBuilder, path, deviceId, locationId))
					.accept(MediaType.APPLICATION_JSON)
					.headers(headers -> headers.setBearerAuth(accessToken))
					.exchange((request, response) -> {
						HttpStatusCode statusCode = response.getStatusCode();
						String body = StreamUtils.copyToString(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);
						log.info("SmartThings GET {} completed status={} durationMs={}",
								endpoint, statusCode.value(), Duration.between(startedAt, Instant.now()).toMillis());
						if (statusCode.isError()) {
							throw toApiException(statusCode, body);
						}
						return parseBody(body);
					});
		} catch (SmartThingsApiException exception) {
			throw exception;
		} catch (ResourceAccessException exception) {
			log.warn("SmartThings GET {} failed due to network error durationMs={}",
					endpoint, Duration.between(startedAt, Instant.now()).toMillis());
			throw new SmartThingsApiException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"SMARTTHINGS_NETWORK_ERROR",
					"Could not connect to SmartThings API.");
		} catch (RestClientException exception) {
			log.warn("SmartThings GET {} failed due to client error durationMs={}",
					endpoint, Duration.between(startedAt, Instant.now()).toMillis());
			throw new SmartThingsApiException(
					HttpStatus.BAD_GATEWAY,
					"SMARTTHINGS_CLIENT_ERROR",
					"SmartThings API request failed.");
		}
	}

	private URI buildUri(
			org.springframework.web.util.UriBuilder uriBuilder,
			String path,
			String deviceId,
			String locationId) {
		if (deviceId == null) {
			return uriBuilder.path(path).queryParam("locationId", locationId).build();
		}
		return uriBuilder.path(path).build(deviceId);
	}

	private JsonNode parseBody(String body) {
		try {
			return objectMapper.readTree(body);
		} catch (JsonProcessingException exception) {
			throw new SmartThingsApiException(
					HttpStatus.BAD_GATEWAY,
					"SMARTTHINGS_INVALID_RESPONSE",
					"SmartThings API returned invalid JSON.");
		}
	}

	private SmartThingsApiException toApiException(HttpStatusCode statusCode, String body) {
		int status = statusCode.value();
		if (status == 401) {
			return new SmartThingsApiException(HttpStatus.UNAUTHORIZED, "SMARTTHINGS_UNAUTHORIZED",
					"SmartThings API rejected the access token.");
		}
		if (status == 403) {
			return new SmartThingsApiException(HttpStatus.FORBIDDEN, "SMARTTHINGS_FORBIDDEN",
					"SmartThings API access is forbidden.");
		}
		if (status == 404) {
			return new SmartThingsApiException(HttpStatus.NOT_FOUND, "SMARTTHINGS_NOT_FOUND",
					"SmartThings resource was not found.");
		}
		if (status == 429) {
			return new SmartThingsApiException(HttpStatus.TOO_MANY_REQUESTS, "SMARTTHINGS_RATE_LIMITED",
					"SmartThings API rate limit exceeded.");
		}
		if (status >= 500) {
			return new SmartThingsApiException(HttpStatus.BAD_GATEWAY, "SMARTTHINGS_SERVER_ERROR",
					"SmartThings API returned a server error.");
		}
		return new SmartThingsApiException(HttpStatus.BAD_GATEWAY, "SMARTTHINGS_HTTP_ERROR",
				"SmartThings API request failed with status " + status + ".");
	}

	private String safeDeviceId(String deviceId) {
		if (deviceId == null || deviceId.length() <= 8) {
			return "device";
		}
		return deviceId.substring(0, 8) + "...";
	}
}
