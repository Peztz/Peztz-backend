package com.peztz.backend.smartthings.device;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class SmartThingsCapabilityResolverTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final SmartThingsCapabilityResolver resolver = new SmartThingsCapabilityResolver();

	@Test
	void classifiesMultipurposeContactSensorWithoutTreatingTemperatureAloneAsHumiditySensor() throws Exception {
		JsonNode device = deviceWithCapabilities("contactSensor", "temperatureMeasurement", "battery");

		assertThat(resolver.supportedTypes(device))
				.containsExactly(SmartThingsDeviceType.CONTACT);
	}

	@Test
	void classifiesTemperatureHumiditySensorOnlyWhenBothCapabilitiesExist() throws Exception {
		JsonNode device = deviceWithCapabilities(
				"temperatureMeasurement", "relativeHumidityMeasurement", "battery");

		assertThat(resolver.supportedTypes(device))
				.containsExactly(SmartThingsDeviceType.TEMPERATURE_HUMIDITY);
	}

	private JsonNode deviceWithCapabilities(String... capabilities) throws Exception {
		String capabilityJson = java.util.Arrays.stream(capabilities)
				.map(capability -> "{\"id\":\"" + capability + "\"}")
				.collect(java.util.stream.Collectors.joining(","));
		return objectMapper.readTree("""
				{
				  "components": [
				    {"id": "main", "capabilities": [%s]}
				  ]
				}
				""".formatted(capabilityJson));
	}
}
