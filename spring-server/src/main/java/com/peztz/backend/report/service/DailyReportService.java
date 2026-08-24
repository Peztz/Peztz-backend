package com.peztz.backend.report.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.repository.AdmissionSessionRepository;
import com.peztz.backend.admission.service.AdmissionSessionService;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.integration.fastapi.FastApiReportClient;
import com.peztz.backend.integration.fastapi.FastApiReportGenerationRequest;
import com.peztz.backend.integration.fastapi.FastApiReportGenerationResponse;
import com.peztz.backend.pet.repository.PetRepository;
import com.peztz.backend.report.dto.BehaviorCardResponse;
import com.peztz.backend.report.dto.DailyReportResponse;
import com.peztz.backend.report.dto.EnvironmentCardResponse;
import com.peztz.backend.report.entity.DailyReport;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyReportService {

	public static final String STATUS_GENERATING = "GENERATING";
	public static final String STATUS_READY = "READY";
	public static final String STATUS_FAILED = "FAILED";
	private static final int MAX_EVENTS_FOR_AI = 200;
	private static final Set<String> RISK_LEVELS = Set.of("NORMAL", "ATTENTION", "URGENT");

	private final PetRepository petRepository;
	private final AdmissionSessionRepository admissionSessionRepository;
	private final AuthService authService;
	private final AdmissionSessionService admissionSessionService;
	private final DailyReportSourceService sourceService;
	private final DailyReportStore reportStore;
	private final FastApiReportClient fastApiReportClient;
	private final ObjectMapper objectMapper;

	@Value("${peztz.report.time-zone:Asia/Seoul}")
	private String reportTimeZone;

	@Value("${peztz.report.retry-after-minutes:10}")
	private long retryAfterMinutes;

	@Value("${peztz.report.generation-lease-seconds:210}")
	private long generationLeaseSeconds;

	@Value("${peztz.report.generation-wait-seconds:220}")
	private long generationWaitSeconds;

	@Value("${peztz.report.generation-poll-millis:500}")
	private long generationPollMillis;

	@Value("${peztz.report.model-label:${OPENAI_MODEL:gpt-5-mini}}")
	private String modelLabel;

	public DailyReportResponse getByPet(String authorization, UUID petId, LocalDate date) {
		AppUser owner = authService.requireUser(authorization);
		petRepository.findByIdAndOwnerId(petId, owner.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
		validateDate(date);
		return getOrGenerate(petId, date);
	}

	public DailyReportResponse getBySession(String authorization, Long sessionId, LocalDate date) {
		AdmissionSession session = admissionSessionService.getOwnedSession(authorization, sessionId);
		validateDate(date);
		return getOrGenerate(session.getPet().getId(), date);
	}

	@Transactional(readOnly = true)
	public List<UUID> findScheduledPetIds(LocalDate date) {
		DateRange range = dateRange(date);
		return admissionSessionRepository.findPetIdsWithSessionOverlapping(range.start(), range.endExclusive());
	}

	public DailyReportResponse generateScheduledReport(UUID petId, LocalDate date) {
		if (!petRepository.existsById(petId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found");
		}
		return getOrGenerate(petId, date);
	}

	private DailyReportResponse getOrGenerate(UUID petId, LocalDate date) {
		DateRange range = dateRange(date);
		DailyReportSource source = sourceService.load(petId, range.start(), range.endExclusive());
		DailyReportStatistics statistics = calculateStatistics(source);
		OffsetDateTime now = OffsetDateTime.now();

		Optional<DailyReportClaim> claim = reportStore.tryClaim(
				petId,
				date,
				statistics,
				now,
				now.minusMinutes(retryAfterMinutes),
				now.minusSeconds(generationLeaseSeconds));
		if (claim.isPresent()) {
			return generateAndComplete(claim.orElseThrow(), source, date, statistics);
		}

		DailyReport existing = reportStore.findByPetAndDate(petId, date)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.SERVICE_UNAVAILABLE, "Daily report claim is temporarily unavailable"));
		if (!STATUS_GENERATING.equals(existing.getStatus())) {
			return toResponse(existing);
		}
		return waitForCompletedReport(existing.getId());
	}

	private DailyReportResponse generateAndComplete(
			DailyReportClaim claim,
			DailyReportSource source,
			LocalDate date,
			DailyReportStatistics statistics) {
		FastApiReportGenerationResponse content;
		String status = STATUS_READY;
		String errorMessage = null;
		boolean hasSourceData = statistics.totalLogCount() > 0;

		if (!hasSourceData) {
			content = noDataContent();
		} else {
			try {
				content = fastApiReportClient.generate(toGenerationRequest(source, date, statistics));
			} catch (RuntimeException exception) {
				status = STATUS_FAILED;
				errorMessage = shortError(exception);
				content = failedContent();
			}
		}

		boolean completed = reportStore.complete(
				claim,
				status,
				objectMapper.convertValue(content, new TypeReference<Map<String, Object>>() { }),
				hasSourceData ? modelLabel : null,
				errorMessage,
				OffsetDateTime.now(ZoneId.of(reportTimeZone)).withNano(0));
		if (!completed) {
			return waitForCompletedReport(claim.reportId());
		}
		return reportStore.findById(claim.reportId())
				.map(this::toResponse)
				.orElseThrow(() -> new IllegalStateException("Completed daily report was not found"));
	}

	private DailyReportResponse waitForCompletedReport(UUID reportId) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(generationWaitSeconds);
		do {
			Optional<DailyReport> report = reportStore.findById(reportId);
			if (report.isPresent() && !STATUS_GENERATING.equals(report.orElseThrow().getStatus())) {
				return toResponse(report.orElseThrow());
			}
			sleepBeforePolling();
		} while (System.nanoTime() < deadline);

		throw new ResponseStatusException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Daily report is still generating; retry shortly");
	}

	private void sleepBeforePolling() {
		try {
			Thread.sleep(generationPollMillis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ResponseStatusException(
					HttpStatus.SERVICE_UNAVAILABLE, "Daily report wait was interrupted", exception);
		}
	}

	private DailyReportStatistics calculateStatistics(DailyReportSource source) {
		List<Double> temperatures = new ArrayList<>();
		List<Double> humidities = new ArrayList<>();
		long petLogSensorCount = 0;
		long doorOpenCount = 0;
		long lowLightCount = 0;

		for (DailyReportSource.LogObservation log : source.logs()) {
			if ("SENSOR".equals(log.type())) {
				petLogSensorCount++;
			}
			if ("DOOR_OPEN".equals(log.type())) {
				doorOpenCount++;
			}
			if ("LOW_LIGHT".equals(log.type())) {
				lowLightCount++;
			}
			addIfPresent(temperatures, numberValue(log.data().get("temperature")));
			addIfPresent(humidities, numberValue(log.data().get("humidity")));
		}

		for (DailyReportSource.SensorMeasurement measurement : source.sensorMeasurements()) {
			if ("temperature".equalsIgnoreCase(measurement.attribute())) {
				temperatures.add(measurement.numericValue());
			} else if ("humidity".equalsIgnoreCase(measurement.attribute())) {
				humidities.add(measurement.numericValue());
			}
		}

		long smartThingsSensorCount = source.sensorMeasurements().size();
		return new DailyReportStatistics(
				source.logs().size() + smartThingsSensorCount,
				petLogSensorCount + smartThingsSensorCount,
				roundedAverage(temperatures),
				roundedAverage(humidities),
				doorOpenCount,
				lowLightCount);
	}

	private void addIfPresent(List<Double> values, Double value) {
		if (value != null) {
			values.add(value);
		}
	}

	private Double roundedAverage(List<Double> values) {
		if (values.isEmpty()) {
			return null;
		}
		double average = values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
		return Math.round(average * 10.0) / 10.0;
	}

	private FastApiReportGenerationRequest toGenerationRequest(
			DailyReportSource source,
			LocalDate date,
			DailyReportStatistics statistics) {
		List<TimedEvent> timedEvents = new ArrayList<>();
		for (DailyReportSource.LogObservation log : source.logs()) {
			timedEvents.add(new TimedEvent(
					log.occurredAt(),
					new FastApiReportGenerationRequest.Event(
							log.type(),
							log.occurredAt().toString(),
							log.durationSeconds(),
							stringValue(log.data().get("message")),
							numberValue(log.data().get("temperature")),
							numberValue(log.data().get("humidity")),
							numberValue(log.data().get("confidence")))));
		}
		for (DailyReportSource.SensorMeasurement measurement : source.sensorMeasurements()) {
			timedEvents.add(new TimedEvent(
					measurement.measuredAt(),
					toSensorEvent(measurement)));
		}

		timedEvents.sort(Comparator.comparing(TimedEvent::occurredAt));
		int fromIndex = Math.max(0, timedEvents.size() - MAX_EVENTS_FOR_AI);
		List<FastApiReportGenerationRequest.Event> events = timedEvents.subList(fromIndex, timedEvents.size()).stream()
				.map(TimedEvent::event)
				.toList();

		DailyReportSource.PetProfile pet = source.pet();
		return new FastApiReportGenerationRequest(
				date,
				pet.name(),
				pet.breed(),
				pet.birthDate(),
				new FastApiReportGenerationRequest.Statistics(
						statistics.totalLogCount(),
						statistics.sensorLogCount(),
						statistics.averageTemperature(),
						statistics.averageHumidity(),
						statistics.doorOpenCount(),
						statistics.lowLightCount()),
				events);
	}

	private FastApiReportGenerationRequest.Event toSensorEvent(
			DailyReportSource.SensorMeasurement measurement) {
		boolean temperature = "temperature".equalsIgnoreCase(measurement.attribute());
		String type = temperature ? "SENSOR_TEMPERATURE" : "SENSOR_HUMIDITY";
		String label = temperature ? "SmartThings 온도 측정" : "SmartThings 습도 측정";
		return new FastApiReportGenerationRequest.Event(
				type,
				measurement.measuredAt().toString(),
				null,
				label,
				temperature ? measurement.numericValue() : null,
				temperature ? null : measurement.numericValue(),
				null);
	}

	private Double numberValue(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		if (value instanceof String text) {
			try {
				return Double.valueOf(text);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	private FastApiReportGenerationResponse noDataContent() {
		return new FastApiReportGenerationResponse(
				"이 날짜에는 분석할 수 있는 관찰 또는 환경 측정 기록이 없습니다.",
				List.of(),
				new FastApiReportGenerationResponse.EnvironmentCard(
						"생활 환경", "환경 센서 측정값이 충분하지 않습니다."),
				List.of("카메라와 센서가 정상적으로 연결되어 있는지 확인해 주세요."),
				"NORMAL",
				List.of("데이터가 없어 반려동물의 상태를 판단할 수 없습니다."),
				"이 리포트는 진단이 아닌 관찰 데이터 요약입니다.");
	}

	private FastApiReportGenerationResponse failedContent() {
		return new FastApiReportGenerationResponse(
				"관찰 및 환경 통계는 수집되었지만 AI 분석을 완료하지 못했습니다.",
				List.of(),
				new FastApiReportGenerationResponse.EnvironmentCard(
						"생활 환경", "아래의 수집 통계를 확인해 주세요."),
				List.of("잠시 후 리포트를 다시 열어 분석을 재시도해 주세요."),
				"NORMAL",
				List.of("AI 분석 서버와 통신하지 못했습니다."),
				"이 리포트는 진단이 아닌 관찰 데이터 요약입니다.");
	}

	private DailyReportResponse toResponse(DailyReport report) {
		FastApiReportGenerationResponse content = objectMapper.convertValue(
				report.getContent(), FastApiReportGenerationResponse.class);
		List<BehaviorCardResponse> behaviorCards = content.behaviorCards() == null
				? List.of()
				: content.behaviorCards().stream()
						.map(card -> new BehaviorCardResponse(
								card.title(), card.description(), card.evidence() == null ? List.of() : card.evidence()))
						.toList();
		FastApiReportGenerationResponse.EnvironmentCard environment = content.environmentCard();
		EnvironmentCardResponse environmentCard = new EnvironmentCardResponse(
				environment == null ? "생활 환경" : environment.title(),
				environment == null ? "환경 분석 결과가 없습니다." : environment.description(),
				report.getAverageTemperature(),
				report.getAverageHumidity(),
				report.getDoorOpenCount(),
				report.getLowLightCount());

		String riskLevel = content.riskLevel() != null && RISK_LEVELS.contains(content.riskLevel())
				? content.riskLevel()
				: "NORMAL";
		return new DailyReportResponse(
				report.getId(),
				report.getPet().getId(),
				report.getPet().getName(),
				report.getReportDate(),
				report.getStatus(),
				report.getTotalLogCount(),
				report.getSensorLogCount(),
				report.getAverageTemperature(),
				report.getAverageHumidity(),
				content.summary(),
				behaviorCards,
				environmentCard,
				content.careTips() == null ? List.of() : content.careTips(),
				riskLevel,
				content.warnings() == null ? List.of() : content.warnings(),
				content.disclaimer(),
				report.getGeneratedAt());
	}

	private void validateDate(LocalDate date) {
		if (date.isAfter(LocalDate.now(ZoneId.of(reportTimeZone)))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report date must not be in the future");
		}
	}

	private DateRange dateRange(LocalDate date) {
		ZoneId zone = ZoneId.of(reportTimeZone);
		return new DateRange(
				date.atStartOfDay(zone).toOffsetDateTime(),
				date.plusDays(1).atStartOfDay(zone).toOffsetDateTime());
	}

	private String shortError(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return message.length() > 500 ? message.substring(0, 500) : message;
	}

	private record DateRange(OffsetDateTime start, OffsetDateTime endExclusive) {
	}

	private record TimedEvent(
			OffsetDateTime occurredAt,
			FastApiReportGenerationRequest.Event event) {
	}
}
