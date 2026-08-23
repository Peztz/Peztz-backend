package com.peztz.backend.smartthings.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.common.GlobalExceptionHandler;
import com.peztz.backend.smartthings.client.SmartThingsClient;
import com.peztz.backend.smartthings.config.SmartThingsProperties;
import com.peztz.backend.smartthings.device.SmartThingsCapabilityResolver;
import com.peztz.backend.smartthings.device.SmartThingsDevice;
import com.peztz.backend.smartthings.device.SmartThingsDeviceRepository;
import com.peztz.backend.smartthings.device.SmartThingsDeviceType;
import com.peztz.backend.smartthings.exception.SmartThingsExceptionHandler;
import com.peztz.backend.smartthings.service.SmartThingsService;

class SmartThingsControllerTest {

	private static final String PEZTZ_TOKEN = "Bearer peztz-token";
	private static final String SMARTTHINGS_TOKEN = "smartthings-token";
	private static final String LOCATION_ID = "813afc24-6a5d-4106-9630-47c69ff6f9d7";

	private AuthService authService;
	private SmartThingsClient smartThingsClient;
	private SmartThingsDeviceRepository deviceRepository;
	private SmartThingsProperties properties;
	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		authService = Mockito.mock(AuthService.class);
		smartThingsClient = Mockito.mock(SmartThingsClient.class);
		deviceRepository = Mockito.mock(SmartThingsDeviceRepository.class);
		properties = new SmartThingsProperties();
		properties.setAccessToken(SMARTTHINGS_TOKEN);
		properties.setLocationId(LOCATION_ID);
		objectMapper = new ObjectMapper();
		SmartThingsService service = new SmartThingsService(
				authService,
				smartThingsClient,
				properties,
				deviceRepository,
				new SmartThingsCapabilityResolver());
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
	void rejectsMissingSmartThingsLocationAfterPeztzAuthorization() throws Exception {
		properties.setLocationId("");
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
		UUID cageId = UUID.randomUUID();
		UUID mappingId = UUID.randomUUID();
		when(deviceRepository.findByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(
				SmartThingsDevice.builder()
						.id(mappingId)
						.cage(Cage.builder().id(cageId).name("B-1").build())
						.smartThingsDeviceId("device-1")
						.deviceType(SmartThingsDeviceType.ILLUMINANCE)
						.label("B-1 light")
						.online(true)
						.active(true)
						.build()));
		when(smartThingsClient.getDevices(SMARTTHINGS_TOKEN, LOCATION_ID)).thenReturn(objectMapper.readTree("""
				{
				  "items": [
				    {
				      "deviceId": "device-1",
				      "name": "sensor",
				      "label": "Sensor",
				      "manufacturerName": "SmartThings",
				      "components": [
				        {
				          "id": "main",
				          "capabilities": [
				            {"id": "illuminanceMeasurement"},
				            {"id": "battery"}
				          ]
				        }
				      ],
				      "sensitiveMetadata": "must-not-be-returned"
				    }
				  ],
				  "paging": {"next": "cursor"},
				  "sensitiveMetadata": "must-not-be-returned"
				}
				"""));

		mockMvc.perform(get("/api/smartthings/devices")
						.header("Authorization", PEZTZ_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].deviceId").value("device-1"))
				.andExpect(jsonPath("$.items[0].supportedTypes[0]").value("ILLUMINANCE"))
				.andExpect(jsonPath("$.items[0].registered").value(true))
				.andExpect(jsonPath("$.items[0].mapping.mappingId").value(mappingId.toString()))
				.andExpect(jsonPath("$.items[0].mapping.cageId").value(cageId.toString()))
				.andExpect(jsonPath("$.items[0].mapping.cageName").value("B-1"))
				.andExpect(jsonPath("$.paging.next").value("cursor"))
				.andExpect(jsonPath("$.items[0].raw").doesNotExist())
				.andExpect(jsonPath("$.raw").doesNotExist());

		verify(smartThingsClient).getDevices(SMARTTHINGS_TOKEN, LOCATION_ID);
	}

	@Test
	void allowsFacilityManagerRoleForStatusLookup() throws Exception {
		when(authService.requireUser(PEZTZ_TOKEN)).thenReturn(user("FACILITY_MANAGER"));
		when(smartThingsClient.getDevice(SMARTTHINGS_TOKEN, "device-1")).thenReturn(objectMapper.readTree("""
				{
				  "deviceId": "device-1",
				  "locationId": "813afc24-6a5d-4106-9630-47c69ff6f9d7"
				}
				"""));
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
				.andExpect(jsonPath("$.components.main.customCapability.customAttribute.value").value("preserved"))
				.andExpect(jsonPath("$.raw").doesNotExist());

		verify(smartThingsClient).getDevice(SMARTTHINGS_TOKEN, "device-1");
		verify(smartThingsClient).getDeviceStatus(SMARTTHINGS_TOKEN, "device-1");
	}

	@Test
	void rejectsStatusLookupForDeviceInAnotherLocation() throws Exception {
		when(authService.requireUser(PEZTZ_TOKEN)).thenReturn(user("ADMIN"));
		when(smartThingsClient.getDevice(SMARTTHINGS_TOKEN, "personal-device"))
				.thenReturn(objectMapper.readTree("""
						{
						  "deviceId": "personal-device",
						  "locationId": "personal-location"
						}
						"""));

		mockMvc.perform(get("/api/smartthings/devices/personal-device/status")
						.header("Authorization", PEZTZ_TOKEN))
				.andExpect(status().isNotFound());

		verify(smartThingsClient).getDevice(SMARTTHINGS_TOKEN, "personal-device");
		verify(smartThingsClient, never()).getDeviceStatus(SMARTTHINGS_TOKEN, "personal-device");
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
