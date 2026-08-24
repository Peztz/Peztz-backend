package com.peztz.backend.facility;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.common.GlobalExceptionHandler;
import com.peztz.backend.facility.controller.FacilityLogController;
import com.peztz.backend.facility.dto.FacilityLogResponse;
import com.peztz.backend.facility.service.FacilityLogService;

class FacilityLogControllerTest {

	private static final String AUTHORIZATION = "Bearer facility-token";

	private FacilityLogService facilityLogService;
	private MockMvc mockMvc;
	private UUID facilityId;

	@BeforeEach
	void setUp() {
		facilityLogService = Mockito.mock(FacilityLogService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new FacilityLogController(facilityLogService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
		facilityId = UUID.randomUUID();
	}

	@Test
	void returnsFacilityLogsUsingDefaultLimit() throws Exception {
		FacilityLogResponse response = new FacilityLogResponse(
				136L,
				1000000010L,
				UUID.randomUUID(),
				"뉴케이지",
				"B-1",
				UUID.randomUUID(),
				"마루",
				"DOOR_OPEN",
				"SENSOR",
				"WARNING",
				"Cage door opened",
				OffsetDateTime.parse("2026-08-24T21:00:59+09:00"));
		when(facilityLogService.findLogs(AUTHORIZATION, facilityId, 100))
				.thenReturn(List.of(response));

		mockMvc.perform(get("/api/facilities/{facilityId}/logs", facilityId)
					.header("Authorization", AUTHORIZATION))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].logId").value(136))
				.andExpect(jsonPath("$[0].sessionId").value(1000000010L))
				.andExpect(jsonPath("$[0].cageName").value("뉴케이지"))
				.andExpect(jsonPath("$[0].petName").value("마루"))
				.andExpect(jsonPath("$[0].category").value("SENSOR"))
				.andExpect(jsonPath("$[0].level").value("WARNING"));

		verify(facilityLogService).findLogs(AUTHORIZATION, facilityId, 100);
	}

	@Test
	void forwardsRequestedLimit() throws Exception {
		when(facilityLogService.findLogs(AUTHORIZATION, facilityId, 25))
				.thenReturn(List.of());

		mockMvc.perform(get("/api/facilities/{facilityId}/logs", facilityId)
					.header("Authorization", AUTHORIZATION)
					.param("limit", "25"))
				.andExpect(status().isOk());

		verify(facilityLogService).findLogs(AUTHORIZATION, facilityId, 25);
	}

	@Test
	void returnsForbiddenWhenFacilityAccessIsRejected() throws Exception {
		when(facilityLogService.findLogs(AUTHORIZATION, facilityId, 100))
				.thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User cannot access this facility"));

		mockMvc.perform(get("/api/facilities/{facilityId}/logs", facilityId)
					.header("Authorization", AUTHORIZATION))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}
}
