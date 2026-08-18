package com.peztz.backend.camera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.entity.AuthToken;
import com.peztz.backend.auth.repository.AppUserRepository;
import com.peztz.backend.auth.repository.AuthTokenRepository;
import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.repository.AdmissionSessionRepository;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.cage.repository.CageRepository;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:peztz_camera_event_test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"peztz.internal-api-key=test-internal-key",
		"peztz.fastapi.client-mode=mock"
})
@AutoConfigureMockMvc
class CameraPetEventControllerTest {

	private static final String OWNER_TOKEN = "camera-owner-token";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	AppUserRepository appUserRepository;

	@Autowired
	AuthTokenRepository authTokenRepository;

	@Autowired
	PetRepository petRepository;

	@Autowired
	CageRepository cageRepository;

	@Autowired
	AdmissionSessionRepository admissionSessionRepository;

	AppUser owner;
	Pet pet;
	Cage cage;
	AdmissionSession session;

	@BeforeEach
	void setUp() {
		createTables();
		deleteRows();
		owner = saveUser("owner@example.com");
		saveToken(OWNER_TOKEN, owner);
		pet = petRepository.save(Pet.builder()
				.id(UUID.randomUUID())
				.owner(owner)
				.name("Choco")
				.breed("Poodle")
				.build());
		cage = cageRepository.save(Cage.builder()
				.id(UUID.randomUUID())
				.user(owner)
				.currentPet(pet)
				.name("Living room cage")
				.status("OCCUPIED")
				.build());
		session = admissionSessionRepository.save(AdmissionSession.builder()
				.owner(owner)
				.pet(pet)
				.cage(cage)
				.accessCode("123456")
				.status("ACTIVE")
				.build());
	}

	@Test
	void ownerCanManageCameraAndReadFastApiEventsWithoutRtspSecretExposure() throws Exception {
		MvcResult cameraResult = mockMvc.perform(post("/api/cameras")
					.header("Authorization", "Bearer " + OWNER_TOKEN)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of(
							"cageId", cage.getId(),
							"name", "Living Room",
							"rtspConfigKey", "pi-secret/camera-1"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cageId").value(cage.getId().toString()))
				.andExpect(jsonPath("$.name").value("Living Room"))
				.andExpect(jsonPath("$.status").value("REGISTERED"))
				.andExpect(jsonPath("$.streamStatus").value("IDLE"))
				.andExpect(jsonPath("$.rtspConfigured").value(true))
				.andExpect(jsonPath("$.rtspConfigKey").doesNotExist())
				.andReturn();
		JsonNode camera = objectMapper.readTree(cameraResult.getResponse().getContentAsString());
		UUID cameraId = UUID.fromString(camera.get("cameraId").asText());

		mockMvc.perform(get("/api/cameras/my")
					.header("Authorization", "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].cameraId").value(cameraId.toString()));

		mockMvc.perform(get("/api/cameras/{cameraId}", cameraId)
					.header("Authorization", "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cameraId").value(cameraId.toString()));

		mockMvc.perform(put("/api/cameras/{cameraId}", cameraId)
					.header("Authorization", "Bearer " + OWNER_TOKEN)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name":"Bedroom","rtspConfigKey":null}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Bedroom"))
				.andExpect(jsonPath("$.rtspConfigured").value(false));

		mockMvc.perform(get("/api/cameras/{cameraId}/runtime-status", cameraId)
					.header("Authorization", "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IDLE"))
				.andExpect(jsonPath("$.playbackUrl").doesNotExist());

		mockMvc.perform(post("/api/cameras")
					.header("Authorization", "Bearer " + OWNER_TOKEN)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of(
							"cageId", cage.getId(),
							"name", "Duplicate camera"))))
				.andExpect(status().isConflict());

		String eventBody = objectMapper.writeValueAsString(Map.ofEntries(
				Map.entry("externalEventId", "edge-event-001"),
				Map.entry("cameraId", cameraId),
				Map.entry("eventType", "excessive_barking"),
				Map.entry("confidence", 0.93),
				Map.entry("occurredAt", "2026-07-28T10:15:30+09:00"),
				Map.entry("eventEndedAt", "2026-07-28T10:16:00+09:00"),
				Map.entry("clipStartAt", "2026-07-28T10:15:20+09:00"),
				Map.entry("clipEndAt", "2026-07-28T10:16:10+09:00"),
				Map.entry("videoUrl", "gs://peztz-events/video/event-001.mp4"),
				Map.entry("thumbnailUrl", "gs://peztz-events/thumbnail/event-001.jpg"),
				Map.entry("metadata", Map.of("model", "mock-yolo"))));

		mockMvc.perform(post("/api/internal/pet-events")
					.header("X-Internal-Api-Key", "wrong-key")
					.contentType(MediaType.APPLICATION_JSON)
					.content(eventBody))
				.andExpect(status().isUnauthorized());

		MvcResult eventResult = mockMvc.perform(post("/api/internal/pet-events")
					.header("X-Internal-Api-Key", "test-internal-key")
					.contentType(MediaType.APPLICATION_JSON)
					.content(eventBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventType").value("EXCESSIVE_BARKING"))
				.andExpect(jsonPath("$.confidence").value(0.93))
				.andExpect(jsonPath("$.createdAt").value("2026-07-28T01:15:30Z"))
				.andExpect(jsonPath("$.eventEndedAt").value("2026-07-28T01:16:00Z"))
				.andExpect(jsonPath("$.eventDurationSeconds").value(30))
				.andExpect(jsonPath("$.clipStartAt").value("2026-07-28T01:15:20Z"))
				.andExpect(jsonPath("$.clipEndAt").value("2026-07-28T01:16:10Z"))
				.andExpect(jsonPath("$.clipDurationSeconds").value(50))
				.andExpect(jsonPath("$.videoUrl").value("gs://peztz-events/video/event-001.mp4"))
				.andExpect(jsonPath("$.thumbnailUrl").value("gs://peztz-events/thumbnail/event-001.jpg"))
				.andReturn();
		JsonNode event = objectMapper.readTree(eventResult.getResponse().getContentAsString());
		long eventId = event.get("eventId").asLong();
		assertThat(jdbcTemplate.queryForObject(
				"select log_type from public.pet_logs where log_id = ?",
				String.class,
				eventId)).isEqualTo("EXCESSIVE_BARKING");

		mockMvc.perform(post("/api/internal/pet-events")
					.header("X-Internal-Api-Key", "test-internal-key")
					.contentType(MediaType.APPLICATION_JSON)
					.content(eventBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventId").value(eventId));

		jdbcTemplate.update(
				"insert into public.pet_logs(session_id, log_type, data, created_at) values (?, ?, '{}', current_timestamp)",
				session.getId(),
				"TEMPERATURE");

		mockMvc.perform(get("/api/pet-events/my")
					.header("Authorization", "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].eventId").value(eventId));

		mockMvc.perform(get("/api/pet-events/{eventId}", eventId)
					.header("Authorization", "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.petId").value(pet.getId().toString()))
				.andExpect(jsonPath("$.metadata.model").value("mock-yolo"));
	}

	@Test
	void anotherOwnerCannotReadCameraOrEvent() throws Exception {
		AppUser otherOwner = saveUser("other@example.com");
		saveToken("other-token", otherOwner);

		MvcResult cameraResult = mockMvc.perform(post("/api/cameras")
					.header("Authorization", "Bearer " + OWNER_TOKEN)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of(
							"cageId", cage.getId(),
							"name", "Private Camera"))))
				.andExpect(status().isOk())
				.andReturn();
		UUID cameraId = UUID.fromString(objectMapper.readTree(
				cameraResult.getResponse().getContentAsString()).get("cameraId").asText());

		mockMvc.perform(get("/api/cameras/{cameraId}", cameraId)
					.header("Authorization", "Bearer other-token"))
				.andExpect(status().isNotFound());
	}

	@Test
	void raspberryPiRegistrationRequiresInternalKeyAndRejectsPublicIp() throws Exception {
		String publicIpBody = objectMapper.writeValueAsString(Map.of(
				"macAddress", "00:11:22:33:44:55",
				"lastIp", "8.8.8.8"));

		mockMvc.perform(post("/api/raspberrypis/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(publicIpBody))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/raspberrypis/register")
					.header("X-Internal-Api-Key", "test-internal-key")
					.contentType(MediaType.APPLICATION_JSON)
					.content(publicIpBody))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/raspberrypis/register")
					.header("X-Internal-Api-Key", "test-internal-key")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of(
							"macAddress", "00:11:22:33:44:55",
							"lastIp", "100.64.10.20"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.macAddress").value("00:11:22:33:44:55"))
				.andExpect(jsonPath("$.lastIp").value("100.64.10.20"));
	}

	private AppUser saveUser(String email) {
		return appUserRepository.save(AppUser.builder()
				.id(UUID.randomUUID())
				.email(email)
				.passwordHash("password")
				.name("Owner")
				.role("OWNER")
				.build());
	}

	private void saveToken(String tokenValue, AppUser user) {
		authTokenRepository.save(AuthToken.builder()
				.id(UUID.randomUUID())
				.token(tokenValue)
				.user(user)
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusDays(1))
				.build());
	}

	private void createTables() {
		jdbcTemplate.execute("create schema if not exists public");
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
				create table if not exists public."Pets" (
					pet_id uuid primary key,
					user_id uuid not null,
					name varchar(50) not null,
					pet_breed varchar(50) not null,
					birth_date date,
					medical_note text
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.raspberrypi (
					device_id uuid primary key,
					mac_address varchar(255),
					last_ip varchar(255),
					is_active varchar(255),
					last_ping time
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
				create table if not exists public.camera (
					camera_id uuid primary key,
					cage_id uuid not null unique,
					name varchar(100) not null,
					status varchar(50) not null,
					stream_status varchar(50) not null,
					rtsp_config_key varchar(255),
					created_at timestamp with time zone not null,
					updated_at timestamp with time zone not null
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
				create table if not exists public.pet_videos (
					video_id bigint generated by default as identity primary key,
					session_id bigint not null,
					video_path text,
					thumbnail_path text,
					start_time timestamp with time zone,
					end_time timestamp with time zone,
					duration integer,
					created_at timestamp with time zone not null
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
	}

	private void deleteRows() {
		jdbcTemplate.update("delete from public.pet_logs");
		jdbcTemplate.update("delete from public.pet_videos");
		jdbcTemplate.update("delete from public.access_session");
		jdbcTemplate.update("delete from public.camera");
		jdbcTemplate.update("delete from public.cage");
		jdbcTemplate.update("delete from public.auth_token");
		jdbcTemplate.update("delete from public.\"Pets\"");
		jdbcTemplate.update("delete from public.raspberrypi");
		jdbcTemplate.update("delete from public.users");
	}
}
