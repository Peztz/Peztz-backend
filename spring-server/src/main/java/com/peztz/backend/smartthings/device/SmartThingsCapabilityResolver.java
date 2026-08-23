package com.peztz.backend.smartthings.device;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class SmartThingsCapabilityResolver {

	private static final String CONTACT_SENSOR = "contactSensor";
	private static final String ILLUMINANCE_MEASUREMENT = "illuminanceMeasurement";
	private static final String TEMPERATURE_MEASUREMENT = "temperatureMeasurement";
	private static final String HUMIDITY_MEASUREMENT = "relativeHumidityMeasurement";

	public List<SmartThingsDeviceType> supportedTypes(JsonNode device) {
		Set<String> capabilities = capabilityIds(device.path("components"));
		List<SmartThingsDeviceType> supported = new java.util.ArrayList<>();
		if (capabilities.contains(CONTACT_SENSOR)) {
			supported.add(SmartThingsDeviceType.CONTACT);
		}
		if (capabilities.contains(ILLUMINANCE_MEASUREMENT)) {
			supported.add(SmartThingsDeviceType.ILLUMINANCE);
		}
		if (capabilities.contains(TEMPERATURE_MEASUREMENT)
				&& capabilities.contains(HUMIDITY_MEASUREMENT)) {
			supported.add(SmartThingsDeviceType.TEMPERATURE_HUMIDITY);
		}
		return List.copyOf(supported);
	}

	private Set<String> capabilityIds(JsonNode components) {
		Set<String> capabilities = new LinkedHashSet<>();
		if (components.isArray()) {
			components.forEach(component -> collectComponentCapabilities(component, capabilities));
		} else if (components.isObject()) {
			components.elements().forEachRemaining(component -> collectComponentCapabilities(component, capabilities));
		}
		return capabilities;
	}

	private void collectComponentCapabilities(JsonNode component, Set<String> capabilities) {
		JsonNode declaredCapabilities = component.path("capabilities");
		if (declaredCapabilities.isArray()) {
			declaredCapabilities.forEach(capability -> {
				if (capability.isTextual()) {
					capabilities.add(capability.asText());
				} else if (capability.path("id").isTextual()) {
					capabilities.add(capability.path("id").asText());
				}
			});
		}

		// Also accepts the component shape returned by the device status API.
		if (component.isObject()) {
			component.fieldNames().forEachRemaining(field -> {
				if (!"capabilities".equals(field) && !"categories".equals(field) && !"id".equals(field)) {
					capabilities.add(field);
				}
			});
		}
	}
}
