package com.peztz.backend.smartthings.sensor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.repository.AdmissionSessionRepository;
import com.peztz.backend.admission.service.AdmissionSessionService;
import com.peztz.backend.log.entity.SessionLog;
import com.peztz.backend.log.repository.SessionLogRepository;
import com.peztz.backend.smartthings.config.SmartThingsProperties;
import com.peztz.backend.smartthings.device.SmartThingsDevice;
import com.peztz.backend.smartthings.device.SmartThingsDeviceRepository;
import com.peztz.backend.smartthings.device.SmartThingsDeviceType;
import com.peztz.backend.smartthings.exception.SmartThingsApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SensorIngestionService {

	private final SmartThingsDeviceRepository deviceRepository;
	private final SensorReadingRepository readingRepository;
	private final AdmissionSessionRepository admissionSessionRepository;
	private final SessionLogRepository sessionLogRepository;
	private final SmartThingsSensorParser parser;
	private final SensorReadingMapper readingMapper;
	private final SmartThingsProperties properties;
	private final ObjectMapper objectMapper;

	@Transactional
	public SensorIngestionResult ingest(UUID mappingId, JsonNode status, String source) {
		SmartThingsDevice device = deviceRepository.findById(mappingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SmartThings device mapping not found"));
		List<SensorSnapshot> snapshots = parser.parse(device.getDeviceType(), status);
		if (snapshots.isEmpty()) {
			throw new SmartThingsApiException(
					HttpStatus.BAD_GATEWAY,
					"SMARTTHINGS_SENSOR_DATA_MISSING",
					"SmartThings status did not contain the expected sensor measurement.");
		}

		OffsetDateTime syncedAt = OffsetDateTime.now().withNano(0);
		Integer battery = parser.extractBattery(status);
		if (battery != null) {
			device.setBattery(battery);
		}
		device.setOnline(true);
		device.setLastSeenAt(syncedAt);

		Optional<AdmissionSession> activeSession = admissionSessionRepository
				.findFirstByCageIdAndStatusOrderByCreatedAtDesc(
						device.getCage().getId(), AdmissionSessionService.STATUS_ACTIVE);
		Map<String, Object> rawPayload = objectMapper.convertValue(
				status, new TypeReference<Map<String, Object>>() { });
		String normalizedSource = normalizeSource(source);

		List<SensorReading> savedReadings = snapshots.stream()
				.map(snapshot -> saveIfNew(
						device, activeSession.orElse(null), snapshot, normalizedSource, rawPayload))
				.flatMap(Optional::stream)
				.toList();

		return new SensorIngestionResult(
				device.getSmartThingsDeviceId(),
				syncedAt,
				savedReadings.stream().map(readingMapper::toResponse).toList());
	}

	private Optional<SensorReading> saveIfNew(
			SmartThingsDevice device,
			AdmissionSession activeSession,
			SensorSnapshot snapshot,
			String source,
			Map<String, Object> rawPayload) {
		if (readingRepository.existsByDeviceIdAndCapabilityAndAttributeAndMeasuredAt(
				device.getId(), snapshot.capability(), snapshot.attribute(), snapshot.measuredAt())) {
			return Optional.empty();
		}

		Optional<SensorReading> previous = readingRepository
				.findFirstByDeviceIdAndCageIdAndCapabilityAndAttributeOrderByMeasuredAtDesc(
						device.getId(), device.getCage().getId(), snapshot.capability(), snapshot.attribute());
		AdmissionSession measurementSession = sessionForMeasurement(activeSession, snapshot.measuredAt());
		SensorReading reading = readingRepository.save(SensorReading.builder()
				.device(device)
				.cage(device.getCage())
				.session(measurementSession)
				.capability(snapshot.capability())
				.attribute(snapshot.attribute())
				.numericValue(snapshot.numericValue())
				.stringValue(snapshot.stringValue())
				.unit(snapshot.unit())
				.measuredAt(snapshot.measuredAt())
				.source(source)
				.rawPayload(new LinkedHashMap<>(rawPayload))
				.build());

		boolean chronologicallyNewest = previous
				.map(existing -> snapshot.measuredAt().isAfter(existing.getMeasuredAt()))
				.orElse(true);
		if (measurementSession != null && chronologicallyNewest) {
			createDerivedEventIfNeeded(device, measurementSession, snapshot, previous.orElse(null));
		}
		return Optional.of(reading);
	}

	private AdmissionSession sessionForMeasurement(
			AdmissionSession activeSession,
			OffsetDateTime measuredAt) {
		if (activeSession == null || measuredAt.isBefore(activeSession.getCreatedAt())) {
			return null;
		}
		if (activeSession.getEndedAt() != null && measuredAt.isAfter(activeSession.getEndedAt())) {
			return null;
		}
		return activeSession;
	}

	private void createDerivedEventIfNeeded(
			SmartThingsDevice device,
			AdmissionSession session,
			SensorSnapshot current,
			SensorReading previous) {
		String eventType = derivedEventType(device.getDeviceType(), current, previous);
		if (eventType == null) {
			return;
		}

		String externalEventId = "st:%s:%s:%d".formatted(
				device.getId(), current.attribute(), current.measuredAt().toInstant().toEpochMilli());
		if (sessionLogRepository.findByExternalEventId(externalEventId).isPresent()) {
			return;
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", eventMessage(eventType, current));
		data.put("smartThingsDeviceId", device.getSmartThingsDeviceId());
		data.put("sensorType", device.getDeviceType().name());
		data.put("capability", current.capability());
		data.put("attribute", current.attribute());
		data.put("value", current.numericValue() == null ? current.stringValue() : current.numericValue());
		if (current.unit() != null) {
			data.put("unit", current.unit());
		}
		if (device.getBattery() != null) {
			data.put("battery", device.getBattery());
		}

		sessionLogRepository.save(SessionLog.builder()
				.session(session)
				.externalEventId(externalEventId)
				.type(eventType)
				.data(data)
				.createdAt(current.measuredAt())
				.build());
	}

	private String derivedEventType(
			SmartThingsDeviceType deviceType,
			SensorSnapshot current,
			SensorReading previous) {
		if (deviceType == SmartThingsDeviceType.CONTACT) {
			String currentValue = normalizeText(current.stringValue());
			String previousValue = previous == null ? null : normalizeText(previous.getStringValue());
			if ("open".equals(currentValue) && !"open".equals(previousValue)) {
				return "DOOR_OPEN";
			}
			if ("closed".equals(currentValue) && "open".equals(previousValue)) {
				return "DOOR_CLOSED";
			}
			return null;
		}

		if (deviceType == SmartThingsDeviceType.ILLUMINANCE && current.numericValue() != null) {
			BigDecimal threshold = properties.getLowLightThresholdLux();
			boolean currentLow = current.numericValue().compareTo(threshold) < 0;
			boolean previousLow = previous != null
					&& previous.getNumericValue() != null
					&& previous.getNumericValue().compareTo(threshold) < 0;
			if (currentLow && !previousLow) {
				return "LOW_LIGHT";
			}
			if (!currentLow && previousLow) {
				return "LIGHT_RECOVERED";
			}
		}
		return null;
	}

	private String eventMessage(String eventType, SensorSnapshot snapshot) {
		return switch (eventType) {
			case "DOOR_OPEN" -> "Cage door opened";
			case "DOOR_CLOSED" -> "Cage door closed";
			case "LOW_LIGHT" -> "Cage illuminance fell below %s lux (current: %s lux)".formatted(
					properties.getLowLightThresholdLux(), snapshot.numericValue());
			case "LIGHT_RECOVERED" -> "Cage illuminance recovered to %s lux".formatted(snapshot.numericValue());
			default -> eventType;
		};
	}

	private String normalizeText(String value) {
		return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeSource(String source) {
		return source == null || source.isBlank() ? "MANUAL" : source.trim().toUpperCase(Locale.ROOT);
	}
}
