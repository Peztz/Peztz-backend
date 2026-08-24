package com.peztz.backend.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.facility.dto.FacilityLogResponse;
import com.peztz.backend.facility.entity.Facility;
import com.peztz.backend.facility.service.FacilityLogService;
import com.peztz.backend.facility.service.FacilityService;
import com.peztz.backend.log.entity.SessionLog;
import com.peztz.backend.log.repository.SessionLogRepository;
import com.peztz.backend.pet.entity.Pet;

@ExtendWith(MockitoExtension.class)
class FacilityLogServiceTest {

	private static final String AUTHORIZATION = "Bearer facility-token";

	@Mock
	private AuthService authService;
	@Mock
	private FacilityService facilityService;
	@Mock
	private SessionLogRepository sessionLogRepository;

	private FacilityLogService service;
	private UUID facilityId;

	@BeforeEach
	void setUp() {
		service = new FacilityLogService(authService, facilityService, sessionLogRepository);
		facilityId = UUID.randomUUID();
		when(facilityService.getFacility(facilityId))
				.thenReturn(Facility.builder().id(facilityId).name("다정 동물병원").build());
	}

	@Test
	void facilityManagerCanReadOwnFacilityLogsWithDisplayMetadata() {
		when(authService.requireUser(AUTHORIZATION)).thenReturn(user("FACILITY_MANAGER", facilityId));
		SessionLog lowLight = log(136L, "LOW_LIGHT", "Cage illuminance fell below 50 lux", false);
		SessionLog pacing = log(137L, "PACING", null, true);
		when(sessionLogRepository.findByFacilityIdOrderByCreatedAtDesc(
				facilityId, PageRequest.of(0, 100)))
				.thenReturn(List.of(pacing, lowLight));

		List<FacilityLogResponse> response = service.findLogs(AUTHORIZATION, facilityId, 100);

		assertThat(response).hasSize(2);
		assertThat(response.get(0).logId()).isEqualTo(137L);
		assertThat(response.get(0).category()).isEqualTo("BEHAVIOR");
		assertThat(response.get(0).level()).isEqualTo("WARNING");
		assertThat(response.get(0).message()).isEqualTo("PACING 이벤트가 감지되었습니다.");
		assertThat(response.get(0).cageName()).isEqualTo("뉴케이지");
		assertThat(response.get(0).cageNumber()).isEqualTo("B-1");
		assertThat(response.get(0).petName()).isEqualTo("마루");
		assertThat(response.get(1).category()).isEqualTo("SENSOR");
		assertThat(response.get(1).message()).isEqualTo("Cage illuminance fell below 50 lux");
	}

	@Test
	void adminCanReadAnyFacilityLogs() {
		when(authService.requireUser(AUTHORIZATION)).thenReturn(user("ADMIN", null));
		when(sessionLogRepository.findByFacilityIdOrderByCreatedAtDesc(
				facilityId, PageRequest.of(0, 20)))
				.thenReturn(List.of());

		assertThat(service.findLogs(AUTHORIZATION, facilityId, 20)).isEmpty();

		verify(sessionLogRepository)
				.findByFacilityIdOrderByCreatedAtDesc(facilityId, PageRequest.of(0, 20));
	}

	@Test
	void facilityManagerCannotReadAnotherFacilityLogs() {
		when(authService.requireUser(AUTHORIZATION))
				.thenReturn(user("FACILITY_MANAGER", UUID.randomUUID()));

		assertThatThrownBy(() -> service.findLogs(AUTHORIZATION, facilityId, 100))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception ->
						assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

		verify(sessionLogRepository, never())
				.findByFacilityIdOrderByCreatedAtDesc(facilityId, PageRequest.of(0, 100));
	}

	@Test
	void ownerCannotReadFacilityLogs() {
		when(authService.requireUser(AUTHORIZATION)).thenReturn(user("OWNER", null));

		assertThatThrownBy(() -> service.findLogs(AUTHORIZATION, facilityId, 100))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception ->
						assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void rejectsLimitOutsideSupportedRange() {
		when(authService.requireUser(AUTHORIZATION)).thenReturn(user("ADMIN", null));

		assertThatThrownBy(() -> service.findLogs(AUTHORIZATION, facilityId, 201))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(exception.getReason()).isEqualTo("limit must be between 1 and 200");
				});
	}

	private AppUser user(String role, UUID hospitalId) {
		return AppUser.builder()
				.id(UUID.randomUUID())
				.hospitalId(hospitalId)
				.email("user@example.com")
				.name("사용자")
				.role(role)
				.build();
	}

	private SessionLog log(Long id, String type, String message, boolean cameraEvent) {
		AppUser owner = AppUser.builder().id(UUID.randomUUID()).name("보호자").build();
		Pet pet = Pet.builder().id(UUID.randomUUID()).owner(owner).name("마루").breed("말티즈").build();
		Cage cage = Cage.builder()
				.id(UUID.randomUUID())
				.facility(Facility.builder().id(facilityId).name("다정 동물병원").build())
				.name("뉴케이지")
				.cageNumber("B-1")
				.build();
		AdmissionSession session = AdmissionSession.builder()
				.id(1000000010L)
				.owner(owner)
				.pet(pet)
				.cage(cage)
				.build();
		return SessionLog.builder()
				.id(id)
				.session(session)
				.camera(cameraEvent ? com.peztz.backend.camera.entity.Camera.builder().id(UUID.randomUUID()).build() : null)
				.type(type)
				.data(message == null ? Map.of() : Map.of("message", message))
				.createdAt(OffsetDateTime.parse("2026-08-24T21:00:59+09:00"))
				.build();
	}
}
