package com.peztz.backend.smartthings.sensor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.smartthings.device.SmartThingsDeviceType;

class SmartThingsSensorParserTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final SmartThingsSensorParser parser = new SmartThingsSensorParser();

	@Test
	void parsesVerifiedIlluminanceAndContactShapes() throws IOException {
		JsonNode illuminance = fixture("illuminance-status.json");
		JsonNode contact = fixture("contact-open-status.json");

		List<SensorSnapshot> lightSnapshots = parser.parse(SmartThingsDeviceType.ILLUMINANCE, illuminance);
		List<SensorSnapshot> contactSnapshots = parser.parse(SmartThingsDeviceType.CONTACT, contact);

		assertThat(lightSnapshots).singleElement().satisfies(snapshot -> {
			assertThat(snapshot.capability()).isEqualTo("illuminanceMeasurement");
			assertThat(snapshot.attribute()).isEqualTo("illuminance");
			assertThat(snapshot.numericValue()).isEqualByComparingTo(BigDecimal.valueOf(381));
			assertThat(snapshot.unit()).isEqualTo("lux");
		});
		assertThat(parser.extractBattery(illuminance)).isEqualTo(90);

		assertThat(contactSnapshots).singleElement().satisfies(snapshot -> {
			assertThat(snapshot.capability()).isEqualTo("contactSensor");
			assertThat(snapshot.attribute()).isEqualTo("contact");
			assertThat(snapshot.stringValue()).isEqualTo("open");
		});
		assertThat(parser.extractBattery(contact)).isEqualTo(87);
	}

	@Test
	void parsesTemperatureAndHumidityForFutureCompatibleSensor() throws IOException {
		List<SensorSnapshot> snapshots = parser.parse(
				SmartThingsDeviceType.TEMPERATURE_HUMIDITY,
				fixture("temperature-humidity-status.json"));

		assertThat(snapshots).hasSize(2);
		assertThat(snapshots).extracting(SensorSnapshot::attribute)
				.containsExactly("temperature", "humidity");
		assertThat(snapshots).extracting(SensorSnapshot::numericValue)
				.containsExactly(BigDecimal.valueOf(24.6), BigDecimal.valueOf(58.2));
	}

	private JsonNode fixture(String name) throws IOException {
		try (InputStream input = getClass().getResourceAsStream("/smartthings/" + name)) {
			if (input == null) {
				throw new IOException("Missing fixture " + name);
			}
			return objectMapper.readTree(input);
		}
	}
}
