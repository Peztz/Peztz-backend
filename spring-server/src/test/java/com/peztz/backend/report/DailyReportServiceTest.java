package com.peztz.backend.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.peztz.backend.report.dto.DailyReportResponse;
import com.peztz.backend.report.entity.DailyReport;
import com.peztz.backend.report.repository.DailyReportRepository;
import com.peztz.backend.report.service.DailyReportService;

@ExtendWith(MockitoExtension.class)
class DailyReportServiceTest {

	@Mock
	private SessionLogRepository sessionLogRepository;
	@Mock
	private PetRepository petRepository;
	@Mock
	private DailyReportRepository dailyReportRepository;
	@Mock
	private AdmissionSessionRepository admissionSessionRepository;
	@Mock
	private AuthService authService;
	@Mock
	private AdmissionSessionService admissionSessionService;
	@Mock
	private FastApiReportClient fastApiReportClient;

	private DailyReportService service;
	private AppUser owner;
	private Pet pet;
	private LocalDate reportDate;

	@BeforeEach
	void setUp() {
		service = new DailyReportService(
				sessionLogRepository,
				petRepository,
				dailyReportRepository,
				admissionSessionRepository,
				authService,
				admissionSessionService,
				fastApiReportClient,
				new ObjectMapper().findAndRegisterModules());
		ReflectionTestUtils.setField(service, "reportTimeZone", "Asia/Seoul");
		ReflectionTestUtils.setField(service, "retryAfterMinutes", 10L);
		ReflectionTestUtils.setField(service, "modelLabel", "gpt-5-mini");

		owner = AppUser.builder().id(UUID.randomUUID()).name("보호자").build();
		pet = Pet.builder()
				.id(UUID.randomUUID())
				.owner(owner)
				.name("초코")
				.breed("Poodle")
				.birthDate(LocalDate.of(2022, 3, 1))
				.build();
		reportDate = LocalDate.of(2026, 8, 22);

		when(authService.requireUser("Bearer token")).thenReturn(owner);
		when(petRepository.findByIdAndOwnerId(pet.getId(), owner.getId())).thenReturn(Optional.of(pet));
		when(dailyReportRepository.findByPetIdAndReportDate(pet.getId(), reportDate))
				.thenReturn(Optional.empty());
		when(dailyReportRepository.saveAndFlush(any(DailyReport.class))).thenAnswer(invocation -> {
			DailyReport report = invocation.getArgument(0);
			report.setId(UUID.randomUUID());
			report.setUpdatedAt(OffsetDateTime.now());
			return report;
		});
	}

	@Test
	void generatesAndStoresStructuredReportFromActualPetLogs() {
		SessionLog log = SessionLog.builder()
				.type("DOOR_OPEN")
				.data(Map.of("message", "케이지 문 열림", "confidence", 0.92))
				.createdAt(OffsetDateTime.parse("2026-08-22T09:10:00+09:00"))
				.build();
		when(sessionLogRepository.findByPetIdAndCreatedAtRange(eq(pet.getId()), any(), any()))
				.thenReturn(List.of(log));
		when(fastApiReportClient.generate(any())).thenReturn(new FastApiReportGenerationResponse(
				"오늘은 전반적으로 안정적인 하루였습니다.",
				List.of(new FastApiReportGenerationResponse.BehaviorCard(
						"문 열림 감지", "한 차례 문 열림이 감지되었습니다.", List.of("09:10 DOOR_OPEN"))),
				new FastApiReportGenerationResponse.EnvironmentCard("생활 환경", "특이사항이 없습니다."),
				List.of("물을 충분히 마시는지 확인해 주세요."),
				"NORMAL",
				List.of(),
				"이 리포트는 진단이 아닌 관찰 데이터 요약입니다."));

		DailyReportResponse response = service.getByPet("Bearer token", pet.getId(), reportDate);

		ArgumentCaptor<FastApiReportGenerationRequest> requestCaptor =
				ArgumentCaptor.forClass(FastApiReportGenerationRequest.class);
		verify(fastApiReportClient).generate(requestCaptor.capture());
		assertThat(requestCaptor.getValue().petName()).isEqualTo("초코");
		assertThat(requestCaptor.getValue().reportDate()).isEqualTo(reportDate);
		assertThat(requestCaptor.getValue().events()).hasSize(1);
		assertThat(requestCaptor.getValue().events().getFirst().confidence()).isEqualTo(0.92);

		assertThat(response.status()).isEqualTo("READY");
		assertThat(response.summary()).contains("안정적인 하루");
		assertThat(response.behaviorCards()).hasSize(1);
		assertThat(response.environmentCard().doorOpenCount()).isEqualTo(1);
		assertThat(response.riskLevel()).isEqualTo("NORMAL");
	}

	@Test
	void doesNotCallOpenAiWhenThereAreNoLogs() {
		when(sessionLogRepository.findByPetIdAndCreatedAtRange(eq(pet.getId()), any(), any()))
				.thenReturn(List.of());

		DailyReportResponse response = service.getByPet("Bearer token", pet.getId(), reportDate);

		verify(fastApiReportClient, never()).generate(any());
		assertThat(response.status()).isEqualTo("READY");
		assertThat(response.totalLogCount()).isZero();
		assertThat(response.summary()).contains("관찰 기록이 없습니다");
		assertThat(response.warnings()).isNotEmpty();
	}

	@Test
	void regeneratesReadyReportWhenLateLogArrives() {
		DailyReport existing = DailyReport.builder()
				.id(UUID.randomUUID())
				.pet(pet)
				.reportDate(reportDate)
				.status("READY")
				.totalLogCount(0)
				.content(Map.of(
						"summary", "기존 리포트",
						"behaviorCards", List.of(),
						"environmentCard", Map.of("title", "생활 환경", "description", "기존 환경"),
						"careTips", List.of(),
						"riskLevel", "NORMAL",
						"warnings", List.of(),
						"disclaimer", "관찰 데이터 요약"))
				.updatedAt(OffsetDateTime.now())
				.build();
		when(dailyReportRepository.findByPetIdAndReportDate(pet.getId(), reportDate))
				.thenReturn(Optional.of(existing));
		when(sessionLogRepository.findByPetIdAndCreatedAtRange(eq(pet.getId()), any(), any()))
				.thenReturn(List.of(SessionLog.builder()
						.type("LOW_LIGHT")
						.data(Map.of("message", "조도 낮음"))
						.createdAt(OffsetDateTime.parse("2026-08-22T23:50:00+09:00"))
						.build()));
		when(fastApiReportClient.generate(any())).thenReturn(new FastApiReportGenerationResponse(
				"늦게 도착한 로그를 반영했습니다.",
				List.of(),
				new FastApiReportGenerationResponse.EnvironmentCard("생활 환경", "저조도 한 건이 감지됐습니다."),
				List.of("조명을 확인해 주세요."),
				"ATTENTION",
				List.of(),
				"관찰 데이터 요약"));

		DailyReportResponse response = service.getByPet("Bearer token", pet.getId(), reportDate);

		verify(fastApiReportClient).generate(any());
		assertThat(response.totalLogCount()).isEqualTo(1);
		assertThat(response.environmentCard().lowLightCount()).isEqualTo(1);
		assertThat(response.summary()).contains("늦게 도착한 로그");
	}
}
