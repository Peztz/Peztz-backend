package com.peztz.backend.smartthings.device;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.peztz.backend.smartthings.client.SmartThingsClient;
import com.peztz.backend.smartthings.config.SmartThingsProperties;
import com.peztz.backend.smartthings.exception.SmartThingsApiException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SmartThingsDeviceRegistrationValidator {

	private final SmartThingsClient smartThingsClient;
	private final SmartThingsProperties properties;
	private final SmartThingsCapabilityResolver capabilityResolver;

	public SmartThingsDeviceType validate(String deviceId, String rawDeviceType) {
		SmartThingsDeviceType deviceType = parseDeviceType(rawDeviceType);
		String accessToken = requireAccessToken();
		String locationId = requireLocationId();
		JsonNode device = smartThingsClient.getDevice(accessToken, deviceId);

		if (!locationId.equals(textOrNull(device, "locationId"))) {
			throw new SmartThingsApiException(
					HttpStatus.NOT_FOUND,
					"SMARTTHINGS_DEVICE_NOT_IN_LOCATION",
					"SmartThings device was not found in the configured location.");
		}

		List<SmartThingsDeviceType> supportedTypes = capabilityResolver.supportedTypes(device);
		if (!supportedTypes.contains(deviceType)) {
			throw new SmartThingsApiException(
					HttpStatus.BAD_REQUEST,
					"SMARTTHINGS_UNSUPPORTED_DEVICE_TYPE",
					"SmartThings device does not support the selected device type.");
		}
		return deviceType;
	}

	private SmartThingsDeviceType parseDeviceType(String rawType) {
		try {
			return SmartThingsDeviceType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"deviceType must be CONTACT, ILLUMINANCE, or TEMPERATURE_HUMIDITY");
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

	private String requireLocationId() {
		if (!StringUtils.hasText(properties.getLocationId())) {
			throw new SmartThingsApiException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"SMARTTHINGS_NOT_CONFIGURED",
					"SmartThings location ID is not configured.");
		}
		return properties.getLocationId().trim();
	}

	private String textOrNull(JsonNode node, String fieldName) {
		JsonNode value = node.get(fieldName);
		return value == null || value.isNull() ? null : value.asText();
	}
}
