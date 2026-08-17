package com.peztz.backend.smartthings.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.smartthings.client.SmartThingsClient;
import com.peztz.backend.smartthings.config.SmartThingsProperties;
import com.peztz.backend.smartthings.dto.SmartThingsDeviceListResponse;
import com.peztz.backend.smartthings.dto.SmartThingsDeviceResponse;
import com.peztz.backend.smartthings.dto.SmartThingsDeviceStatusResponse;
import com.peztz.backend.smartthings.exception.SmartThingsApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmartThingsService {

	private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "FACILITY_MANAGER", "FACILITY", "HOSPITAL");

	private final AuthService authService;
	private final SmartThingsClient smartThingsClient;
	private final SmartThingsProperties properties;

	@Transactional(readOnly = true)
	public SmartThingsDeviceListResponse findDevices(String authorization) {
		requireAllowedUser(authorization);
		JsonNode response = smartThingsClient.getDevices(
				requireSmartThingsAccessToken(), requireSmartThingsLocationId());
		return toDeviceListResponse(response);
	}

	@Transactional(readOnly = true)
	public SmartThingsDeviceStatusResponse getDeviceStatus(String authorization, String deviceId) {
		requireAllowedUser(authorization);
		String accessToken = requireSmartThingsAccessToken();
		String locationId = requireSmartThingsLocationId();
		JsonNode device = smartThingsClient.getDevice(accessToken, deviceId);
		if (!locationId.equals(textOrNull(device, "locationId"))) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"SmartThings device was not found in the configured location");
		}
		JsonNode response = smartThingsClient.getDeviceStatus(accessToken, deviceId);
		return new SmartThingsDeviceStatusResponse(
				deviceId,
				response.path("components"));
	}

	private void requireAllowedUser(String authorization) {
		AppUser user = authService.requireUser(authorization);
		String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
		if (!ALLOWED_ROLES.contains(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SmartThings access is not allowed for this role");
		}
	}

	private String requireSmartThingsAccessToken() {
		if (!StringUtils.hasText(properties.getAccessToken())) {
			throw new SmartThingsApiException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"SMARTTHINGS_NOT_CONFIGURED",
					"SmartThings access token is not configured.");
		}
		return properties.getAccessToken().trim();
	}

	private String requireSmartThingsLocationId() {
		if (!StringUtils.hasText(properties.getLocationId())) {
			throw new SmartThingsApiException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"SMARTTHINGS_NOT_CONFIGURED",
					"SmartThings location ID is not configured.");
		}
		return properties.getLocationId().trim();
	}

	private SmartThingsDeviceListResponse toDeviceListResponse(JsonNode response) {
		JsonNode items = response.path("items");
		List<SmartThingsDeviceResponse> devices = items.isArray()
				? java.util.stream.StreamSupport.stream(items.spliterator(), false)
						.map(this::toDeviceResponse)
						.toList()
				: List.of();
		return new SmartThingsDeviceListResponse(
				devices,
				response.path("paging"),
				response.path("_links"));
	}

	private SmartThingsDeviceResponse toDeviceResponse(JsonNode item) {
		return new SmartThingsDeviceResponse(
				textOrNull(item, "deviceId"),
				textOrNull(item, "name"),
				textOrNull(item, "label"),
				textOrNull(item, "manufacturerName"),
				item.path("components"));
	}

	private String textOrNull(JsonNode node, String fieldName) {
		JsonNode value = node.get(fieldName);
		return value == null || value.isNull() ? null : value.asText();
	}
}
