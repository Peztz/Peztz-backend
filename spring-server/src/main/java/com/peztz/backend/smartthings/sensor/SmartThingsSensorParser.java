package com.peztz.backend.smartthings.sensor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.peztz.backend.smartthings.device.SmartThingsDeviceType;

@Component
public class SmartThingsSensorParser {

	public List<SensorSnapshot> parse(SmartThingsDeviceType deviceType, JsonNode status) {
		List<SensorSnapshot> snapshots = new ArrayList<>();
		switch (deviceType) {
			case CONTACT -> addStringMeasurement(
					snapshots, status, "contactSensor", "contact");
			case ILLUMINANCE -> addNumericMeasurement(
					snapshots, status, "illuminanceMeasurement", "illuminance");
			case TEMPERATURE_HUMIDITY -> {
				addNumericMeasurement(snapshots, status, "temperatureMeasurement", "temperature");
				addNumericMeasurement(snapshots, status, "relativeHumidityMeasurement", "humidity");
			}
		}
		return List.copyOf(snapshots);
	}

	public Integer extractBattery(JsonNode status) {
		JsonNode value = attributeNode(status, "battery", "battery").path("value");
		return value.isNumber() ? value.intValue() : null;
	}

	private void addNumericMeasurement(
			List<SensorSnapshot> snapshots,
			JsonNode status,
			String capability,
			String attribute) {
		JsonNode measurement = attributeNode(status, capability, attribute);
		JsonNode value = measurement.path("value");
		OffsetDateTime measuredAt = timestamp(measurement);
		if (!value.isNumber() || measuredAt == null) {
			return;
		}
		snapshots.add(new SensorSnapshot(
				capability,
				attribute,
				value.decimalValue(),
				null,
				textOrNull(measurement, "unit"),
				measuredAt));
	}

	private void addStringMeasurement(
			List<SensorSnapshot> snapshots,
			JsonNode status,
			String capability,
			String attribute) {
		JsonNode measurement = attributeNode(status, capability, attribute);
		JsonNode value = measurement.path("value");
		OffsetDateTime measuredAt = timestamp(measurement);
		if (!value.isTextual() || measuredAt == null) {
			return;
		}
		snapshots.add(new SensorSnapshot(
				capability,
				attribute,
				null,
				value.asText(),
				textOrNull(measurement, "unit"),
				measuredAt));
	}

	private JsonNode attributeNode(JsonNode status, String capability, String attribute) {
		return status.path("components").path("main").path(capability).path(attribute);
	}

	private OffsetDateTime timestamp(JsonNode measurement) {
		String rawTimestamp = textOrNull(measurement, "timestamp");
		if (rawTimestamp == null) {
			return null;
		}
		try {
			return OffsetDateTime.parse(rawTimestamp);
		} catch (DateTimeParseException ignored) {
			return null;
		}
	}

	private String textOrNull(JsonNode node, String fieldName) {
		JsonNode value = node.get(fieldName);
		return value == null || value.isNull() ? null : value.asText();
	}
}
