package com.peztz.backend.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.admission.repository.AdmissionSessionRepository;
import com.peztz.backend.admission.service.AdmissionSessionService;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.integration.fastapi.FastApiReportClient;
import com.peztz.backend.integration.fastapi.FastApiReportGenerationRequest;
import com.peztz.backend.integration.fastapi.FastApiReportGenerationResponse;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;
import com.peztz.backend.report.dto.DailyReportResponse;
import com.peztz.backend.report.entity.DailyReport;
import com.peztz.backend.report.service.DailyReportClaim;
import com.peztz.backend.report.service.DailyReportService;
import com.peztz.backend.report.service.DailyReportSource;
import com.peztz.backend.report.service.DailyReportSourceService;
import com.peztz.backend.report.service.DailyReportStatistics;
import com.peztz.backend.report.service.DailyReportStore;

@ExtendWith(MockitoExtension.class)
class DailyReportServiceTest {

	@Mock
	private PetRepository petRepository;
	@Mock
	private AdmissionSessionRepository admissionSessionRepository;
	@Mock
	private AuthService authService;
	@Mock
	private AdmissionSessionService admissionSessionService;
	@Mock
	private DailyReportSourceService sourceService;
	@Mock
	private FastApiReportClient fastApiReportClient;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private DailyReportService service;
	private InMemoryDailyReportStore reportStore;
	private AppUser owner;
	private Pet pet;
	private LocalDate reportDate;

	@BeforeEach
	void setUp() {
		owner = AppUser.builder().id(UUID.randomUUID()).name("보호자").build();
		pet = Pet.builder()
				.id(UUID.randomUUID())
				.owner(owner)
				.name("초코")
				.breed("Poodle")
				.birthDate(LocalDate.of(2022, 3, 1))
				.build();
		reportDate = LocalDate.of(2026, 8, 22);
		reportStore = new InMemoryDailyReportStore(pet);
		service = new DailyReportService(
				petRepository,
				admissionSessionRepository,
				authService,
				admissionSessionService,
				sourceService,
				reportStore,
				fastApiReportClient,
				objectMapper);
		ReflectionTestUtils.setField(service, "reportTimeZone", "Asia/Seoul");
		ReflectionTestUtils.setField(service, "retryAfterMinutes", 10L);
		ReflectionTestUtils.setField(service, "generationLeaseSeconds", 210L);
		ReflectionTestUtils.setField(service, "generationWaitSeconds", 3L);
		ReflectionTestUtils.setField(service, "generationPollMillis", 5L);
		ReflectionTestUtils.setField(service, "modelLabel", "gpt-5-mini");

		when(authService.requireUser("Bearer token")).thenReturn(owner);
		when(petRepository.findByIdAndOwnerId(pet.getId(), owner.getId())).thenReturn(Optional.of(pet));
	}

	@Test
	void generatesAiReportWhenOnlyTemperatureAndHumidityReadingsExist() {
		when(sourceService.load(any(), any(), any())).thenReturn(source(
				List.of(),
				List.of(
						new DailyReportSource.SensorMeasurement(
								"temperature", 23.45, "C", OffsetDateTime.parse("2026-08-22T09:00:00+09:00")),
						new DailyReportSource.SensorMeasurement(
								"humidity", 55.55, "%", OffsetDateTime.parse("2026-08-22T09:00:02+09:00")))));
		when(fastApiReportClient.generate(any())).thenReturn(successContent("온습도 측정값을 반영했습니다."));

		DailyReportResponse response = service.getByPet("Bearer token", pet.getId(), reportDate);

		ArgumentCaptor<FastApiReportGenerationRequest> requestCaptor =
				ArgumentCaptor.forClass(FastApiReportGenerationRequest.class);
		verify(fastApiReportClient).generate(requestCaptor.capture());
		FastApiReportGenerationRequest request = requestCaptor.getValue();
		assertThat(request.statistics().totalLogCount()).isEqualTo(2);
		assertThat(request.statistics().sensorLogCount()).isEqualTo(2);
		assertThat(request.statistics().averageTemperature()).isEqualTo(23.5);
		assertThat(request.statistics().averageHumidity()).isEqualTo(55.6);
		assertThat(request.events()).extracting(FastApiReportGenerationRequest.Event::type)
				.containsExactly("SENSOR_TEMPERATURE", "SENSOR_HUMIDITY");
		assertThat(response.status()).isEqualTo("READY");
		assertThat(response.summary()).contains("온습도 측정값");
		assertThat(response.totalLogCount()).isEqualTo(2);
		assertThat(response.environmentCard().averageTemperature()).isEqualTo(23.5);
		assertThat(response.environmentCard().averageHumidity()).isEqualTo(55.6);
	}

	@Test
	void doesNotCallAiOnlyWhenBothObservationAndSensorDataAreMissing() {
		when(sourceService.load(any(), any(), any())).thenReturn(source(List.of(), List.of()));

		DailyReportResponse response = service.getByPet("Bearer token", pet.getId(), reportDate);

		verify(fastApiReportClient, never()).generate(any());
		assertThat(response.status()).isEqualTo("READY");
		assertThat(response.totalLogCount()).isZero();
		assertThat(response.summary()).contains("관찰 또는 환경 측정 기록이 없습니다");
	}

	@Test
	void concurrentRequestsCallAiOnceAndReturnTheSameReportId() throws Exception {
		when(sourceService.load(any(), any(), any())).thenReturn(source(
				List.of(new DailyReportSource.LogObservation(
						"PACING",
						OffsetDateTime.parse("2026-08-22T09:10:00+09:00"),
						null,
						Map.of("confidence", 0.91))),
				List.of()));
		AtomicInteger aiCalls = new AtomicInteger();
		CountDownLatch aiStarted = new CountDownLatch(1);
		CountDownLatch releaseAi = new CountDownLatch(1);
		when(fastApiReportClient.generate(any())).thenAnswer(invocation -> {
			aiCalls.incrementAndGet();
			aiStarted.countDown();
			if (!releaseAi.await(2, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Test did not release the AI response");
			}
			return successContent("동시 요청 결과");
		});

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<DailyReportResponse> first = executor.submit(
					() -> service.getByPet("Bearer token", pet.getId(), reportDate));
			assertThat(aiStarted.await(1, TimeUnit.SECONDS)).isTrue();

			Future<DailyReportResponse> second = executor.submit(
					() -> service.getByPet("Bearer token", pet.getId(), reportDate));
			assertThat(reportStore.awaitClaimAttempts(2, 1, TimeUnit.SECONDS)).isTrue();
			releaseAi.countDown();

			DailyReportResponse firstResponse = first.get(2, TimeUnit.SECONDS);
			DailyReportResponse secondResponse = second.get(2, TimeUnit.SECONDS);
			assertThat(aiCalls).hasValue(1);
			assertThat(firstResponse.reportId()).isEqualTo(secondResponse.reportId());
			assertThat(firstResponse.status()).isEqualTo("READY");
			assertThat(secondResponse.status()).isEqualTo("READY");
		}
	}

	@ParameterizedTest(name = "FastAPI failure is persisted and retry succeeds: {0}")
	@MethodSource("fastApiFailures")
	void failedAndTimedOutAiCallsAreStoredAndRetryWithTheSameReportId(RuntimeException failure) {
		when(sourceService.load(any(), any(), any())).thenReturn(source(
				List.of(new DailyReportSource.LogObservation(
						"PACING",
						OffsetDateTime.parse("2026-08-22T09:10:00+09:00"),
						null,
						Map.of())),
				List.of()));
		when(fastApiReportClient.generate(any()))
				.thenThrow(failure)
				.thenReturn(successContent("재시도에 성공했습니다."));

		DailyReportResponse failed = service.getByPet("Bearer token", pet.getId(), reportDate);

		assertThat(failed.status()).isEqualTo("FAILED");
		assertThat(reportStore.current().getErrorMessage()).isNotBlank();
		reportStore.makeRetryable();

		DailyReportResponse retried = service.getByPet("Bearer token", pet.getId(), reportDate);

		assertThat(retried.reportId()).isEqualTo(failed.reportId());
		assertThat(retried.status()).isEqualTo("READY");
		assertThat(retried.summary()).contains("재시도에 성공");
		verify(fastApiReportClient, org.mockito.Mockito.times(2)).generate(any());
	}

	private static Stream<RuntimeException> fastApiFailures() {
		return Stream.of(
				new IllegalStateException("FastAPI unavailable"),
				new ResourceAccessException("FastAPI read timed out", new SocketTimeoutException("read timed out")));
	}

	private DailyReportSource source(
			List<DailyReportSource.LogObservation> logs,
			List<DailyReportSource.SensorMeasurement> measurements) {
		return new DailyReportSource(
				new DailyReportSource.PetProfile(
						pet.getId(), pet.getName(), pet.getBreed(), pet.getBirthDate()),
				logs,
				measurements);
	}

	private FastApiReportGenerationResponse successContent(String summary) {
		return new FastApiReportGenerationResponse(
				summary,
				List.of(),
				new FastApiReportGenerationResponse.EnvironmentCard("생활 환경", "측정값을 반영했습니다."),
				List.of("물을 충분히 마시는지 확인해 주세요."),
				"NORMAL",
				List.of(),
				"이 리포트는 진단이 아닌 관찰 데이터 요약입니다.");
	}

	private final class InMemoryDailyReportStore implements DailyReportStore {

		private final Pet reportPet;
		private final AtomicInteger claimAttempts = new AtomicInteger();
		private final CountDownLatch secondClaimAttempted = new CountDownLatch(1);
		private DailyReport report;

		private InMemoryDailyReportStore(Pet reportPet) {
			this.reportPet = reportPet;
		}

		@Override
		public synchronized Optional<DailyReportClaim> tryClaim(
				UUID petId,
				LocalDate date,
				DailyReportStatistics statistics,
				OffsetDateTime now,
				OffsetDateTime retryBefore,
				OffsetDateTime staleBefore) {
			if (claimAttempts.incrementAndGet() >= 2) {
				secondClaimAttempted.countDown();
			}

			boolean canClaim = report == null
					|| statisticsChanged(statistics)
					|| (DailyReportService.STATUS_FAILED.equals(report.getStatus())
							&& report.getUpdatedAt().isBefore(retryBefore))
					|| (DailyReportService.STATUS_GENERATING.equals(report.getStatus())
							&& report.getUpdatedAt().isBefore(staleBefore));
			if (!canClaim) {
				return Optional.empty();
			}

			UUID token = UUID.randomUUID();
			if (report == null) {
				report = DailyReport.builder()
						.id(UUID.randomUUID())
						.pet(reportPet)
						.reportDate(date)
						.createdAt(now)
						.build();
			}
			report.setGenerationToken(token);
			report.setStatus(DailyReportService.STATUS_GENERATING);
			report.setTotalLogCount(statistics.totalLogCount());
			report.setSensorLogCount(statistics.sensorLogCount());
			report.setAverageTemperature(statistics.averageTemperature());
			report.setAverageHumidity(statistics.averageHumidity());
			report.setDoorOpenCount(statistics.doorOpenCount());
			report.setLowLightCount(statistics.lowLightCount());
			report.setContent(Map.of());
			report.setModelName(null);
			report.setErrorMessage(null);
			report.setGeneratedAt(null);
			report.setUpdatedAt(now);
			return Optional.of(new DailyReportClaim(report.getId(), token));
		}

		@Override
		public synchronized Optional<DailyReport> findByPetAndDate(UUID petId, LocalDate date) {
			return Optional.ofNullable(report);
		}

		@Override
		public synchronized Optional<DailyReport> findById(UUID reportId) {
			return report != null && report.getId().equals(reportId)
					? Optional.of(report)
					: Optional.empty();
		}

		@Override
		public synchronized boolean complete(
				DailyReportClaim claim,
				String status,
				Map<String, Object> content,
				String modelName,
				String errorMessage,
				OffsetDateTime generatedAt) {
			if (report == null
					|| !DailyReportService.STATUS_GENERATING.equals(report.getStatus())
					|| !claim.generationToken().equals(report.getGenerationToken())) {
				return false;
			}
			report.setGenerationToken(null);
			report.setStatus(status);
			report.setContent(content);
			report.setModelName(modelName);
			report.setErrorMessage(errorMessage);
			report.setGeneratedAt(generatedAt);
			report.setUpdatedAt(OffsetDateTime.now());
			return true;
		}

		private boolean statisticsChanged(DailyReportStatistics statistics) {
			return report != null && !new DailyReportStatistics(
					report.getTotalLogCount(),
					report.getSensorLogCount(),
					report.getAverageTemperature(),
					report.getAverageHumidity(),
					report.getDoorOpenCount(),
					report.getLowLightCount()).equals(statistics);
		}

		private boolean awaitClaimAttempts(int expected, long timeout, TimeUnit unit) throws InterruptedException {
			if (claimAttempts.get() >= expected) {
				return true;
			}
			return secondClaimAttempted.await(timeout, unit);
		}

		private synchronized DailyReport current() {
			return report;
		}

		private synchronized void makeRetryable() {
			report.setUpdatedAt(OffsetDateTime.now().minusMinutes(11));
		}
	}
}
