package com.peztz.backend.integration.fastapi;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name = "peztz.fastapi.client-mode", havingValue = "http")
public class HttpFastApiCameraClient implements FastApiCameraClient {

	private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

	private final RestClient restClient;
	private final String internalApiKey;

	public HttpFastApiCameraClient(
			RestClient.Builder restClientBuilder,
			@Value("${peztz.fastapi.base-url:}") String baseUrl,
			@Value("${peztz.fastapi.internal-api-key:}") String internalApiKey) {
		if (!StringUtils.hasText(baseUrl)) {
			throw new IllegalStateException("peztz.fastapi.base-url is required in http mode");
		}
		this.restClient = restClientBuilder.baseUrl(removeTrailingSlash(baseUrl)).build();
		this.internalApiKey = internalApiKey;
	}

	@Override
	public CameraRuntimeStatusResponse getStatus(UUID cameraId) {
		return exchange("GET", cameraId, "/internal/cameras/{cameraId}/status");
	}

	@Override
	public CameraRuntimeStatusResponse startLiveStream(UUID cameraId) {
		return exchange("POST", cameraId, "/internal/cameras/{cameraId}/live/start");
	}

	@Override
	public CameraRuntimeStatusResponse stopLiveStream(UUID cameraId) {
		return exchange("POST", cameraId, "/internal/cameras/{cameraId}/live/stop");
	}

	private CameraRuntimeStatusResponse exchange(String method, UUID cameraId, String path) {
		if (!StringUtils.hasText(internalApiKey)) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FastAPI internal API key is not configured");
		}
		try {
			CameraRuntimeStatusResponse response;
			if ("GET".equals(method)) {
				response = restClient.get()
						.uri(path, cameraId)
						.header(INTERNAL_API_KEY_HEADER, internalApiKey)
						.retrieve()
						.body(CameraRuntimeStatusResponse.class);
			} else {
				response = restClient.post()
						.uri(path, cameraId)
						.header(INTERNAL_API_KEY_HEADER, internalApiKey)
						.retrieve()
						.body(CameraRuntimeStatusResponse.class);
			}
			if (response == null) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI returned an empty response");
			}
			return response;
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI camera API request failed", exception);
		}
	}

	private String removeTrailingSlash(String value) {
		String trimmed = value.trim();
		return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}
}
