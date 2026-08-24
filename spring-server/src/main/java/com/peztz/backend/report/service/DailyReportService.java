package com.peztz.backend.report.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import com.peztz.backend.log.entity.SessionLog;
import com.peztz.backend.log.repository.SessionLogRepository;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;
import com.peztz.backend.report.dto.BehaviorCardResponse;
import com.peztz.backend.report.dto.DailyReportResponse;
import com.peztz.backend.report.dto.EnvironmentCardResponse;
import com.peztz.backend.report.entity.DailyReport;
import com.peztz.backend.report.repository.DailyReportRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyReportService {

	public static final String STATUS_READY = "READY";
	public static final String STATUS_FAILED = "FAILED";
	private static final int MAX_EVENTS_FOR_AI = 200;
	private static final Set<String> RISK_LEVELS = Set.of("NORMAL", "ATTENTION", "URGENT");

	private final SessionLogRepository sessionLogRepository;
	private final PetRepository petRepository;
	private final DailyReportRepository dailyReportRepository;
	private final AdmissionSessionRepository admissionSessionRepository;
	private final AuthService authService;
	private final AdmissionSessionService admissionSessionService;
	private final FastApiReportClient fastApiReportClient;
	private final ObjectMapper objectMapper;

	@Value("${peztz.report.time-zone:Asia/Seoul}")
	private String reportTimeZone;

	@Value("${peztz.report.retry-after-minutes:10}")
	private long retryAfterMinutes;

	@Value("${peztz.report.model-label:${OPENAI_MODEL:gpt-5-mini}}")
	private String modelLabel;

	@Transactional
	public DailyReportResponse getByPet(String authorization, UUID petId, LocalDate date) {
		AppUser owner = authService.requireUser(authorization);
		Pet pet = petRepository.findByIdAndOwnerId(petId, owner.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
		validateDate(date);
		return getOrGenerate(pet, date);
	}

	@Transactional
	public DailyReportResponse getBySession(String authorization, Long sessionId, LocalDate date) {
		AdmissionSession session = admissionSessionService.getOwnedSession(authorization, sessionId);
		validateDate(date);
		return getOrGenerate(session.getPet(), date);
	}

	@Transactional(readOnly = true)
	public List<UUID> findScheduledPetIds(LocalDate date) {
		DateRange range = dateRange(date);
		return admissionSessionRepository.findPetIdsWithSessionOverlapping(range.start(), range.endExclusive());
	}

	@Transactional
	public DailyReportResponse generateScheduledReport(UUID petId, LocalDate date) {
		return petRepository.findById(petId)
				.map(pet -> getOrGenerate(pet, date))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
	}

	private DailyReportResponse getOrGenerate(Pet pet, LocalDate date) {
		DailyReport existing = dailyReportRepository.findByPetIdAndReportDate(pet.getId(), date).orElse(null);
		DateRange range = dateRange(date);
		List<SessionLog> logs = sessionLogRepository.findByPetIdAndCreatedAtRange(
				pet.getId(), range.start(), range.endExclusive());
		ReportStatistics statistics = calculateStatistics(logs);
		if (existing != null && !shouldRegenerate(existing, statistics)) {
			return toResponse(existing);
		}

		FastApiReportGenerationResponse content;
		String status = STATUS_READY;
		String errorMessage = null;
		if (logs.isEmpty()) {
			content = noDataContent();
		} else {
			try {
				content = fastApiReportClient.generate(toGenerationRequest(pet, date, statistics, logs));
			} catch (RuntimeException exception) {
				status = STATUS_FAILED;
				errorMessage = shortError(exception);
				content = failedContent();
			}
		}

		DailyReport report = existing == null ? new DailyReport() : existing;
		report.setPet(pet);
		report.setReportDate(date);
		report.setStatus(status);
		report.setTotalLogCount(statistics.totalLogCount());
		report.setSensorLogCount(statistics.sensorLogCount());
		report.setAverageTemperature(statistics.averageTemperature());
		report.setAverageHumidity(statistics.averageHumidity());
		report.setDoorOpenCount(statistics.doorOpenCount());
		report.setLowLightCount(statistics.lowLightCount());
		report.setContent(objectMapper.convertValue(content, new TypeReference<Map<String, Object>>() { }));
		report.setModelName(logs.isEmpty() ? null : modelLabel);
		report.setErrorMessage(errorMessage);
		report.setGeneratedAt(OffsetDateTime.now(ZoneId.of(reportTimeZone)).withNano(0));

		return toResponse(dailyReportRepository.saveAndFlush(report));
	}

	private boolean shouldRegenerate(DailyReport report, ReportStatistics statistics) {
		if (STATUS_READY.equals(report.getStatus())) {
			return report.getTotalLogCount() != statistics.totalLogCount();
		}
		if (report.getUpdatedAt() == null) {
			return true;
		}
		return report.getUpdatedAt().isBefore(OffsetDateTime.now().minusMinutes(retryAfterMinutes));
	}

	private ReportStatistics calculateStatistics(List<SessionLog> logs) {
		long sensorCount = logs.stream().filter(log -> "SENSOR".equals(log.getType())).count();
		long doorOpenCount = logs.stream().filter(log -> "DOOR_OPEN".equals(log.getType())).count();
		long lowLightCount = logs.stream().filter(log -> "LOW_LIGHT".equals(log.getType())).count();
		Double averageTemperature = roundedAverage(logs.stream()
				.map(SessionLog::getTemperature).filter(value -> value != null).toList());
		Double averageHumidity = roundedAverage(logs.stream()
				.map(SessionLog::getHumidity).filter(value -> value != null).toList());
		return new ReportStatistics(
				logs.size(), sensorCount, averageTemperature, averageHumidity, doorOpenCount, lowLightCount);
	}

	private Double roundedAverage(List<Double> values) {
		if (values.isEmpty()) {
			return null;
		}
		double average = values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
		return Math.round(average * 10.0) / 10.0;
	}

	private FastApiReportGenerationRequest toGenerationRequest(
			Pet pet, LocalDate date, ReportStatistics statistics, List<SessionLog> logs) {
		int fromIndex = Math.max(0, logs.size() - MAX_EVENTS_FOR_AI);
		List<FastApiReportGenerationRequest.Event> events = logs.subList(fromIndex, logs.size()).stream()
				.map(this::toEvent)
				.toList();
		return new FastApiReportGenerationRequest(
				date,
				pet.getName(),
				pet.getBreed(),
				pet.getBirthDate(),
				new FastApiReportGenerationRequest.Statistics(
						statistics.totalLogCount(),
						statistics.sensorLogCount(),
						statistics.averageTemperature(),
						statistics.averageHumidity(),
						statistics.doorOpenCount(),
						statistics.lowLightCount()),
				events);
	}

	private FastApiReportGenerationRequest.Event toEvent(SessionLog log) {
		return new FastApiReportGenerationRequest.Event(
				log.getType(),
				log.getCreatedAt().toString(),
				log.getEventDurationSeconds(),
				log.getMessage(),
				log.getTemperature(),
				log.getHumidity(),
				numberValue(log.getData() == null ? null : log.getData().get("confidence")));
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

	private FastApiReportGenerationResponse noDataContent() {
		return new FastApiReportGenerationResponse(
				"이 날짜에는 분석할 수 있는 관찰 기록이 없습니다.",
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
				"관찰 통계는 수집되었지만 AI 분석을 완료하지 못했습니다.",
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

	private record ReportStatistics(
			long totalLogCount,
			long sensorLogCount,
			Double averageTemperature,
			Double averageHumidity,
			long doorOpenCount,
			long lowLightCount) {
	}
}
