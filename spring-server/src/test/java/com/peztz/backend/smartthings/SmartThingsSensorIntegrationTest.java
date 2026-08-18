package com.peztz.backend.smartthings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.smartthings.client.SmartThingsClient;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:peztz_smartthings_sensor_test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"smartthings.access-token=test-smartthings-token",
		"smartthings.polling-enabled=false",
		"smartthings.low-light-threshold-lux=50",
		"peztz.fastapi.client-mode=mock"
})
@AutoConfigureMockMvc
class SmartThingsSensorIntegrationTest {

	private static final String AUTH_TOKEN = "facility-smartthings-token";
	private static final String OWNER_AUTH_TOKEN = "owner-smartthings-token";
	private static final String LIGHT_DEVICE_ID = "f629bdb6-304d-42db-8954-428621a80fae";
	private static final String CONTACT_DEVICE_ID = "4667bc8a-35e5-4c0b-9ab0-cdb16e0edbc3";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@MockitoBean
	SmartThingsClient smartThingsClient;

	UUID facilityId;
	UUID managerId;
	UUID ownerId;
	UUID petId;
	UUID cageId;

	@BeforeEach
	void setUp() throws IOException {
		createTables();
		deleteRows();
		insertDomainData();

		when(smartThingsClient.getDeviceStatus("test-smartthings-token", LIGHT_DEVICE_ID))
				.thenReturn(
						fixture("illuminance-status.json"),
						fixture("illuminance-low-status.json"),
						fixture("illuminance-recovered-status.json"));
		when(smartThingsClient.getDeviceStatus("test-smartthings-token", CONTACT_DEVICE_ID))
				.thenReturn(
						fixture("contact-open-status.json"),
						fixture("contact-closed-status.json"));
		when(smartThingsClient.getDeviceHealth("test-smartthings-token", LIGHT_DEVICE_ID))
				.thenReturn(fixture("health-online.json"));
		when(smartThingsClient.getDeviceHealth("test-smartthings-token", CONTACT_DEVICE_ID))
				.thenReturn(fixture("health-online.json"));
	}

	@Test
	void linksSensorsStoresReadingsDeduplicatesAndCreatesSessionEvents() throws Exception {
		registerSensor(LIGHT_DEVICE_ID, "ILLUMINANCE", "Cage light")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cageId").value(cageId.toString()))
				.andExpect(jsonPath("$.deviceId").value(LIGHT_DEVICE_ID))
				.andExpect(jsonPath("$.online").value(false));
		registerSensor(CONTACT_DEVICE_ID, "CONTACT", "Cage door")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deviceType").value("CONTACT"));

		sync(LIGHT_DEVICE_ID).andExpect(status().isOk()).andExpect(jsonPath("$.savedReadingCount").value(1));
		sync(LIGHT_DEVICE_ID).andExpect(status().isOk()).andExpect(jsonPath("$.savedReadingCount").value(1));
		sync(LIGHT_DEVICE_ID).andExpect(status().isOk()).andExpect(jsonPath("$.savedReadingCount").value(1));
		sync(CONTACT_DEVICE_ID).andExpect(status().isOk()).andExpect(jsonPath("$.savedReadingCount").value(1));
		sync(CONTACT_DEVICE_ID).andExpect(status().isOk()).andExpect(jsonPath("$.savedReadingCount").value(1));

		// Mockito repeats the final response. The repeated timestamp must not create another row or event.
		sync(CONTACT_DEVICE_ID)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.savedReadingCount").value(0));

		mockMvc.perform(get("/api/smartthings/cages/{cageId}/readings/latest", cageId)
					.header("Authorization", "Bearer " + AUTH_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readings.length()").value(2));

		mockMvc.perform(get("/api/smartthings/cages/{cageId}/readings", cageId)
					.header("Authorization", "Bearer " + AUTH_TOKEN)
					.param("limit", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5));

		assertThat(jdbcTemplate.queryForObject("select count(*) from public.sensor_reading", Long.class))
				.isEqualTo(5L);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from public.sensor_reading where session_id is not null", Long.class))
				.isEqualTo(5L);
		assertThat(jdbcTemplate.queryForList(
				"select log_type from public.pet_logs order by created_at", String.class))
				.containsExactlyInAnyOrder("LOW_LIGHT", "LIGHT_RECOVERED", "DOOR_OPEN", "DOOR_CLOSED");
		assertThat(jdbcTemplate.queryForObject(
				"select battery from public.smartthings_device where smartthings_device_id = ?",
				Integer.class,
				LIGHT_DEVICE_ID)).isEqualTo(89);
	}

	@Test
	void offlineHealthMarksDeviceOfflineAndSkipsStatusIngestion() throws Exception {
		registerSensor(LIGHT_DEVICE_ID, "ILLUMINANCE", "Cage light")
				.andExpect(status().isOk());
		jdbcTemplate.update(
				"update public.smartthings_device set online = true where smartthings_device_id = ?",
				LIGHT_DEVICE_ID);
		when(smartThingsClient.getDeviceHealth("test-smartthings-token", LIGHT_DEVICE_ID))
				.thenReturn(fixture("health-offline.json"));

		sync(LIGHT_DEVICE_ID)
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("SMARTTHINGS_DEVICE_OFFLINE"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from public.sensor_reading", Long.class))
				.isZero();
		assertThat(jdbcTemplate.queryForObject(
				"select online from public.smartthings_device where smartthings_device_id = ?",
				Boolean.class,
				LIGHT_DEVICE_ID)).isFalse();
		verify(smartThingsClient, never()).getDeviceStatus("test-smartthings-token", LIGHT_DEVICE_ID);
	}

	@Test
	void staleMeasurementIsStoredWithoutCurrentSessionOrDerivedEvent() throws Exception {
		jdbcTemplate.update(
				"update public.access_session set created_at = ? where cage_id = ?",
				OffsetDateTime.parse("2026-08-17T00:00:00Z"), cageId);
		registerSensor(CONTACT_DEVICE_ID, "CONTACT", "Cage door")
				.andExpect(status().isOk());

		sync(CONTACT_DEVICE_ID)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.savedReadingCount").value(1));

		assertThat(jdbcTemplate.queryForObject("select count(*) from public.sensor_reading", Long.class))
				.isEqualTo(1L);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from public.sensor_reading where session_id is not null", Long.class))
				.isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from public.pet_logs", Long.class))
				.isZero();
	}

	@Test
	void outOfOrderMeasurementDoesNotCreateBackwardTransitionEvent() throws Exception {
		when(smartThingsClient.getDeviceStatus("test-smartthings-token", CONTACT_DEVICE_ID))
				.thenReturn(
						fixture("contact-closed-status.json"),
						fixture("contact-open-status.json"));
		registerSensor(CONTACT_DEVICE_ID, "CONTACT", "Cage door")
				.andExpect(status().isOk());

		sync(CONTACT_DEVICE_ID).andExpect(status().isOk());
		sync(CONTACT_DEVICE_ID).andExpect(status().isOk());

		assertThat(jdbcTemplate.queryForObject("select count(*) from public.sensor_reading", Long.class))
				.isEqualTo(2L);
		assertThat(jdbcTemplate.queryForObject("select count(*) from public.pet_logs", Long.class))
				.isZero();
	}

	@Test
	void ownerCannotReadSmartThingsDeviceDetailsOrRawReadings() throws Exception {
		registerSensor(LIGHT_DEVICE_ID, "ILLUMINANCE", "Cage light")
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/smartthings/cages/{cageId}/devices", cageId)
					.header("Authorization", "Bearer " + OWNER_AUTH_TOKEN))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/smartthings/cages/{cageId}/readings/latest", cageId)
					.header("Authorization", "Bearer " + OWNER_AUTH_TOKEN))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/smartthings/cages/{cageId}/readings", cageId)
					.header("Authorization", "Bearer " + OWNER_AUTH_TOKEN))
				.andExpect(status().isForbidden());
	}

	@Test
	void disconnectsAndReassignsDeviceWithoutMixingCageHistoryOrEvents() throws Exception {
		UUID secondCageId = UUID.randomUUID();
		jdbcTemplate.update(
				"insert into public.cage(cage_id, hospital_id, user_id, current_pet_id, status, name, created_at) "
						+ "values (?, ?, ?, ?, ?, ?, ?)",
				secondCageId, facilityId, ownerId, petId, "OCCUPIED", "Cage B", LocalDateTime.now());
		jdbcTemplate.update(
				"insert into public.access_session(access_code, user_id, pet_id, cage_id, status, created_at) "
						+ "values (?, ?, ?, ?, ?, ?)",
				"654321", ownerId, petId, secondCageId, "ACTIVE", OffsetDateTime.parse("2026-08-16T00:00:00Z"));

		registerSensor(CONTACT_DEVICE_ID, "CONTACT", "Cage A door")
				.andExpect(status().isOk());
		sync(CONTACT_DEVICE_ID)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.savedReadingCount").value(1));

		registerSensor(secondCageId, CONTACT_DEVICE_ID, "CONTACT", "Cage B door")
				.andExpect(status().isConflict());

		disconnect(cageId, CONTACT_DEVICE_ID)
				.andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForObject(
				"select active from public.smartthings_device where smartthings_device_id = ?",
				Boolean.class,
				CONTACT_DEVICE_ID)).isFalse();
		assertThat(jdbcTemplate.queryForObject(
				"select online from public.smartthings_device where smartthings_device_id = ?",
				Boolean.class,
				CONTACT_DEVICE_ID)).isFalse();
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from public.smartthings_device where smartthings_device_id = ? and last_seen_at is null",
				Long.class,
				CONTACT_DEVICE_ID)).isEqualTo(1L);

		mockMvc.perform(get("/api/smartthings/cages/{cageId}/devices", cageId)
					.header("Authorization", "Bearer " + AUTH_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		sync(CONTACT_DEVICE_ID).andExpect(status().isConflict());

		registerSensor(secondCageId, CONTACT_DEVICE_ID, "CONTACT", "Cage B door")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cageId").value(secondCageId.toString()))
				.andExpect(jsonPath("$.online").value(false));
		sync(CONTACT_DEVICE_ID)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.savedReadingCount").value(1));

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from public.sensor_reading where cage_id = ?", Long.class, cageId))
				.isEqualTo(1L);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from public.sensor_reading where cage_id = ?", Long.class, secondCageId))
				.isEqualTo(1L);
		assertThat(jdbcTemplate.queryForList(
				"select log_type from public.pet_logs order by created_at", String.class))
				.containsExactly("DOOR_OPEN");
	}

	private org.springframework.test.web.servlet.ResultActions registerSensor(
			String deviceId,
			String deviceType,
			String label) throws Exception {
		return registerSensor(cageId, deviceId, deviceType, label);
	}

	private org.springframework.test.web.servlet.ResultActions registerSensor(
			UUID targetCageId,
			String deviceId,
			String deviceType,
			String label) throws Exception {
		return mockMvc.perform(post("/api/smartthings/cages/{cageId}/devices", targetCageId)
				.header("Authorization", "Bearer " + AUTH_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
						"deviceId", deviceId,
						"deviceType", deviceType,
						"label", label))));
	}

	private org.springframework.test.web.servlet.ResultActions sync(String deviceId) throws Exception {
		return mockMvc.perform(post("/api/smartthings/devices/{deviceId}/sync", deviceId)
				.header("Authorization", "Bearer " + AUTH_TOKEN));
	}

	private org.springframework.test.web.servlet.ResultActions disconnect(UUID targetCageId, String deviceId)
			throws Exception {
		return mockMvc.perform(delete("/api/smartthings/cages/{cageId}/devices/{deviceId}", targetCageId, deviceId)
				.header("Authorization", "Bearer " + AUTH_TOKEN));
	}

	private JsonNode fixture(String name) throws IOException {
		try (InputStream input = getClass().getResourceAsStream("/smartthings/" + name)) {
			if (input == null) {
				throw new IOException("Missing fixture " + name);
			}
			return objectMapper.readTree(input);
		}
	}

	private void insertDomainData() {
		facilityId = UUID.randomUUID();
		managerId = UUID.randomUUID();
		ownerId = UUID.randomUUID();
		petId = UUID.randomUUID();
		cageId = UUID.randomUUID();
		jdbcTemplate.update(
				"insert into public.hospitals(hospital_id, name, phone) values (?, ?, ?)",
				facilityId, "Test facility", "010-0000-0000");
		jdbcTemplate.update(
				"insert into public.users(user_id, hospital_id, email, password, name, role) values (?, ?, ?, ?, ?, ?)",
				managerId, facilityId, "manager@example.com", "password", "Manager", "FACILITY_MANAGER");
		jdbcTemplate.update(
				"insert into public.users(user_id, hospital_id, email, password, name, role) values (?, ?, ?, ?, ?, ?)",
				ownerId, null, "owner@example.com", "password", "Owner", "OWNER");
		jdbcTemplate.update(
				"insert into public.auth_token(id, token, user_id, created_at, expires_at) values (?, ?, ?, ?, ?)",
				UUID.randomUUID(), AUTH_TOKEN, managerId, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
		jdbcTemplate.update(
				"insert into public.auth_token(id, token, user_id, created_at, expires_at) values (?, ?, ?, ?, ?)",
				UUID.randomUUID(), OWNER_AUTH_TOKEN, ownerId, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
		jdbcTemplate.update(
				"insert into public.\"Pets\"(pet_id, user_id, name, pet_breed) values (?, ?, ?, ?)",
				petId, ownerId, "Choco", "Poodle");
		jdbcTemplate.update(
				"insert into public.cage(cage_id, hospital_id, user_id, current_pet_id, status, name, created_at) values (?, ?, ?, ?, ?, ?, ?)",
				cageId, facilityId, ownerId, petId, "OCCUPIED", "Cage A", LocalDateTime.now());
		jdbcTemplate.update(
				"insert into public.access_session(access_code, user_id, pet_id, cage_id, status, created_at) values (?, ?, ?, ?, ?, ?)",
				"123456", ownerId, petId, cageId, "ACTIVE", OffsetDateTime.parse("2026-08-16T00:00:00Z"));
	}

	private void createTables() {
		jdbcTemplate.execute("create schema if not exists public");
		jdbcTemplate.execute("""
				create table if not exists public.hospitals (
					hospital_id uuid primary key,
					name varchar(20) not null,
					phone varchar(20) not null
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.users (
					user_id uuid primary key,
					hospital_id uuid,
					email varchar(50) not null unique,
					password varchar(255) not null,
					name varchar(50) not null,
					role varchar(50) not null
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.auth_token (
					id uuid primary key,
					token varchar(255) not null unique,
					user_id uuid not null,
					created_at timestamp not null,
					expires_at timestamp not null
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.\"Pets\" (
					pet_id uuid primary key,
					user_id uuid not null,
					name varchar(50) not null,
					pet_breed varchar(50) not null,
					birth_date date,
					medical_note text
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.cage (
					cage_id uuid primary key,
					hospital_id uuid,
					user_id uuid,
					device_id uuid,
					current_pet_id uuid,
					access_code varchar(50),
					status varchar(50) not null,
					name varchar(100),
					cage_number varchar(50),
					created_at timestamp not null
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.access_session (
					session_id bigint generated by default as identity primary key,
					access_code varchar(50) not null,
					user_id uuid not null,
					pet_id uuid not null,
					cage_id uuid not null,
					status varchar(50) not null,
					created_at timestamp with time zone not null,
					ended_at timestamp with time zone
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.pet_logs (
					log_id bigint generated by default as identity primary key,
					session_id bigint not null,
					video_id bigint,
					camera_id uuid,
					external_event_id varchar(100) unique,
					log_type varchar(50) not null,
					data json not null,
					created_at timestamp with time zone not null,
					event_ended_at timestamp with time zone,
					event_duration_seconds integer
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.smartthings_device (
					smartthings_device_mapping_id uuid primary key,
					cage_id uuid not null,
					smartthings_device_id varchar(100) not null unique,
					device_type varchar(50) not null,
					label varchar(100),
					battery integer,
					online boolean not null,
					active boolean not null,
					last_seen_at timestamp with time zone,
					created_at timestamp with time zone not null,
					updated_at timestamp with time zone not null
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.sensor_reading (
					reading_id bigint generated by default as identity primary key,
					smartthings_device_mapping_id uuid not null,
					cage_id uuid not null,
					session_id bigint,
					capability varchar(100) not null,
					attribute varchar(100) not null,
					numeric_value numeric(19,4),
					string_value varchar(255),
					unit varchar(30),
					measured_at timestamp with time zone not null,
					received_at timestamp with time zone not null,
					source varchar(30) not null,
					raw_payload json not null,
					unique(smartthings_device_mapping_id, capability, attribute, measured_at)
				)
				""");
	}

	private void deleteRows() {
		for (String table : List.of(
				"sensor_reading", "pet_logs", "smartthings_device", "access_session",
				"cage", "auth_token", "\"Pets\"", "users", "hospitals")) {
			jdbcTemplate.update("delete from public." + table);
		}
	}
}
