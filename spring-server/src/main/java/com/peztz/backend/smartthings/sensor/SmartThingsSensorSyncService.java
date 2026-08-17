package com.peztz.backend.smartthings.sensor;

import java.util.UUID;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.peztz.backend.smartthings.client.SmartThingsClient;
import com.peztz.backend.smartthings.config.SmartThingsProperties;
import com.peztz.backend.smartthings.device.SmartThingsDevice;
import com.peztz.backend.smartthings.device.SmartThingsDeviceRepository;
import com.peztz.backend.smartthings.exception.SmartThingsApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmartThingsSensorSyncService {

	private final SmartThingsDeviceRepository deviceRepository;
	private final SmartThingsClient smartThingsClient;
	private final SmartThingsProperties properties;
	private final SensorIngestionService ingestionService;
	private final SmartThingsDeviceHealthService healthService;

	public SensorIngestionResult sync(UUID mappingId, String source) {
		SmartThingsDevice device = deviceRepository.findById(mappingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SmartThings device mapping not found"));
		try {
			String accessToken = requireAccessToken();
			JsonNode health = smartThingsClient.getDeviceHealth(
					accessToken, device.getSmartThingsDeviceId());
			String healthState = health.path("state").asText("").trim().toUpperCase(Locale.ROOT);
			if (!"ONLINE".equals(healthState)) {
				throw new SmartThingsApiException(
						HttpStatus.SERVICE_UNAVAILABLE,
						"SMARTTHINGS_DEVICE_OFFLINE",
						"SmartThings device is not online (state: %s).".formatted(
								healthState.isEmpty() ? "UNKNOWN" : healthState));
			}
			JsonNode status = smartThingsClient.getDeviceStatus(
					accessToken, device.getSmartThingsDeviceId());
			return ingestionService.ingest(mappingId, status, source);
		} catch (RuntimeException exception) {
			healthService.markOffline(mappingId);
			throw exception;
		}
	}

	private String requireAccessToken() {
		if (!StringUtils.hasText(properties.getAccessToken())) {
			throw new SmartThingsApiException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"SMARTTHINGS_NOT_CONFIGURED",
					"SmartThings access token is not configured.");
		}
		return properties.getAccessToken().trim();
	}
}
