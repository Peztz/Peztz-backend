package com.peztz.backend.smartthings.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.common.GlobalExceptionHandler;
import com.peztz.backend.smartthings.client.SmartThingsClient;
import com.peztz.backend.smartthings.config.SmartThingsProperties;
import com.peztz.backend.smartthings.exception.SmartThingsExceptionHandler;
import com.peztz.backend.smartthings.service.SmartThingsService;

class SmartThingsControllerTest {

	private static final String PEZTZ_TOKEN = "Bearer peztz-token";
	private static final String SMARTTHINGS_TOKEN = "smartthings-token";

	private AuthService authService;
	private SmartThingsClient smartThingsClient;
	private SmartThingsProperties properties;
	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		authService = Mockito.mock(AuthService.class);
		smartThingsClient = Mockito.mock(SmartThingsClient.class);
		properties = new SmartThingsProperties();
		properties.setAccessToken(SMARTTHINGS_TOKEN);
		objectMapper = new ObjectMapper();
		SmartThingsService service = new SmartThingsService(authService, smartThingsClient, properties);
		mockMvc = MockMvcBuilders.standaloneSetup(new SmartThingsController(service))
				.setControllerAdvice(new GlobalExceptionHandler(), new SmartThingsExceptionHandler())
				.build();
	}

	@Test
	void rejectsMissingPeztzAuthorization() throws Exception {
		when(authService.requireUser(null))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header must be Bearer token"));

		mockMvc.perform(get("/api/smartthings/devices"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		verifyNoInteractions(smartThingsClient);
	}

	@Test
	void rejectsOwnerRole() throws Exception {
		when(authService.requireUser(PEZTZ_TOKEN)).thenReturn(user("OWNER"));

		mockMvc.perform(get("/api/smartthings/devices")
						.header("Authorization", PEZTZ_TOKEN))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));

		verifyNoInteractions(smartThingsClient);
	}

	@Test
	void rejectsMissingSmartThingsTokenAfterPeztzAuthorization() throws Exception {
		properties.setAccessToken("");
		when(authService.requireUser(PEZTZ_TOKEN)).thenReturn(user("ADMIN"));

		mockMvc.perform(get("/api/smartthings/devices")
						.header("Authorization", PEZTZ_TOKEN))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("SMARTTHINGS_NOT_CONFIGURED"));

		verifyNoInteractions(smartThingsClient);
	}

	@Test
	void allowsAdminRole() throws Exception {
		when(authService.requireUser(PEZTZ_TOKEN)).thenReturn(user("ADMIN"));
		when(smartThingsClient.getDevices(SMARTTHINGS_TOKEN)).thenReturn(objectMapper.readTree("""
				{
				  "items": [
				    {"deviceId": "device-1", "name": "sensor", "label": "Sensor", "manufacturerName": "SmartThings"}
				  ],
				  "paging": {"next": "cursor"}
				}
				"""));

		mockMvc.perform(get("/api/smartthings/devices")
						.header("Authorization", PEZTZ_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].deviceId").value("device-1"))
				.andExpect(jsonPath("$.paging.next").value("cursor"));

		verify(smartThingsClient).getDevices(SMARTTHINGS_TOKEN);
	}

	@Test
	void allowsFacilityManagerRoleForStatusLookup() throws Exception {
		when(authService.requireUser(PEZTZ_TOKEN)).thenReturn(user("FACILITY_MANAGER"));
		when(smartThingsClient.getDeviceStatus(SMARTTHINGS_TOKEN, "device-1")).thenReturn(objectMapper.readTree("""
				{
				  "components": {
				    "main": {
				      "customCapability": {
				        "customAttribute": {"value": "preserved"}
				      }
				    }
				  }
				}
				"""));

		mockMvc.perform(get("/api/smartthings/devices/device-1/status")
						.header("Authorization", PEZTZ_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deviceId").value("device-1"))
				.andExpect(jsonPath("$.components.main.customCapability.customAttribute.value").value("preserved"));

		verify(smartThingsClient).getDeviceStatus(SMARTTHINGS_TOKEN, "device-1");
	}

	private AppUser user(String role) {
		return AppUser.builder()
				.id(UUID.randomUUID())
				.email("user@example.com")
				.name("User")
				.role(role)
				.build();
	}
}
