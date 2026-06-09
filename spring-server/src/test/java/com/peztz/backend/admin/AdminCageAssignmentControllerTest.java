package com.peztz.backend.admin;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.repository.AdmissionSessionRepository;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.entity.AuthToken;
import com.peztz.backend.auth.repository.AppUserRepository;
import com.peztz.backend.auth.repository.AuthTokenRepository;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.cage.repository.CageRepository;
import com.peztz.backend.cage.service.CageService;
import com.peztz.backend.device.entity.RaspberryPi;
import com.peztz.backend.device.repository.RaspberryPiRepository;
import com.peztz.backend.facility.entity.Facility;
import com.peztz.backend.facility.repository.FacilityRepository;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:peztz_admin_assignment_test;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class AdminCageAssignmentControllerTest {

	private static final String ADMIN_TOKEN = "admin-token";
	private static final String OWNER_TOKEN = "owner-token";

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
	FacilityRepository facilityRepository;

	@Autowired
	CageRepository cageRepository;

	@Autowired
	RaspberryPiRepository raspberryPiRepository;

	@Autowired
	PetRepository petRepository;

	@Autowired
	AdmissionSessionRepository admissionSessionRepository;

	@BeforeEach
	void setUp() {
		createTables();
		deleteRows();
	}

	@Test
	void adminCanCreateFacilityAndItAppearsInFacilityListAndSummary() throws Exception {
		AppUser admin = saveUser("admin@example.com", "Admin", "ADMIN", null);
		saveToken(ADMIN_TOKEN, admin);

		mockMvc.perform(post("/api/admin/facilities")
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"facilityName", "Happy Hospital",
								"phoneNumber", "051-123-4567"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.facilityName").value("Happy Hospital"))
				.andExpect(jsonPath("$.phoneNumber").value("051-123-4567"))
				.andExpect(jsonPath("$.type").value("-"))
				.andExpect(jsonPath("$.cageCount").value(0))
				.andExpect(jsonPath("$.activeSessionCount").value(0))
				.andExpect(jsonPath("$.deviceIssueCount").value(0))
				.andExpect(jsonPath("$.status").value("-"));

		mockMvc.perform(get("/api/admin/facilities")
						.header("Authorization", "Bearer " + ADMIN_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].facilityName").value("Happy Hospital"))
				.andExpect(jsonPath("$[0].phoneNumber").value("051-123-4567"))
				.andExpect(jsonPath("$[0].cageCount").value(0));

		mockMvc.perform(get("/api/admin/summary")
						.header("Authorization", "Bearer " + ADMIN_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalFacilities").value(1));
	}

	@Test
	void facilityNameIsRequiredForAdminFacilityCreate() throws Exception {
		AppUser admin = saveUser("admin@example.com", "Admin", "ADMIN", null);
		saveToken(ADMIN_TOKEN, admin);

		mockMvc.perform(post("/api/admin/facilities")
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("phoneNumber", "051-123-4567"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void adminCanUpdateFacilityAndItAppearsInFacilityList() throws Exception {
		AppUser admin = saveUser("admin@example.com", "Admin", "ADMIN", null);
		saveToken(ADMIN_TOKEN, admin);
		Facility facility = saveFacility("Before Hospital");

		mockMvc.perform(patch("/api/admin/facilities/{facilityId}", facility.getId())
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"facilityName", "After Hospital",
								"phoneNumber", "051-999-9999"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.facilityId").value(facility.getId().toString()))
				.andExpect(jsonPath("$.facilityName").value("After Hospital"))
				.andExpect(jsonPath("$.phoneNumber").value("051-999-9999"))
				.andExpect(jsonPath("$.type").value("-"))
				.andExpect(jsonPath("$.cageCount").value(0))
				.andExpect(jsonPath("$.activeSessionCount").value(0))
				.andExpect(jsonPath("$.deviceIssueCount").value(0))
				.andExpect(jsonPath("$.status").value("-"));

		mockMvc.perform(get("/api/admin/facilities")
						.header("Authorization", "Bearer " + ADMIN_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].facilityId").value(facility.getId().toString()))
				.andExpect(jsonPath("$[0].facilityName").value("After Hospital"))
				.andExpect(jsonPath("$[0].phoneNumber").value("051-999-9999"));
	}

	@Test
	void facilityUpdateRejectsDuplicateNameButAllowsOwnName() throws Exception {
		AppUser admin = saveUser("admin@example.com", "Admin", "ADMIN", null);
		saveToken(ADMIN_TOKEN, admin);
		Facility first = saveFacility("First Hospital");
		Facility second = saveFacility("Second Hospital");

		mockMvc.perform(patch("/api/admin/facilities/{facilityId}", first.getId())
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"facilityName", "First Hospital",
								"phoneNumber", ""))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.facilityName").value("First Hospital"))
				.andExpect(jsonPath("$.phoneNumber").value("-"));

		mockMvc.perform(patch("/api/admin/facilities/{facilityId}", second.getId())
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"facilityName", "First Hospital",
								"phoneNumber", "051-999-9999"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void facilityUpdateReturnsNotFoundForUnknownFacility() throws Exception {
		AppUser admin = saveUser("admin@example.com", "Admin", "ADMIN", null);
		saveToken(ADMIN_TOKEN, admin);

		mockMvc.perform(patch("/api/admin/facilities/{facilityId}", UUID.randomUUID())
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"facilityName", "Missing Hospital",
								"phoneNumber", "051-999-9999"))))
				.andExpect(status().isNotFound());
	}

	@Test
	void facilityNameIsRequiredForAdminFacilityUpdate() throws Exception {
		AppUser admin = saveUser("admin@example.com", "Admin", "ADMIN", null);
		saveToken(ADMIN_TOKEN, admin);
		Facility facility = saveFacility("Hospital");

		mockMvc.perform(patch("/api/admin/facilities/{facilityId}", facility.getId())
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("phoneNumber", "051-999-9999"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void facilityManagerCanUpdateCageAndExistingFacilityOwnerFlowsStillWork() throws Exception {
		Facility facility = saveFacility("Facility Hospital");
		AppUser manager = saveUser("manager@example.com", "Manager", "FACILITY_MANAGER", facility.getId());
		AppUser owner = saveUser("owner@example.com", "Owner", "OWNER", null);
		saveToken("manager-token", manager);
		saveToken(OWNER_TOKEN, owner);
		RaspberryPi oldDevice = raspberryPiRepository.save(RaspberryPi.builder()
				.deviceId(UUID.randomUUID())
				.macAddress("00:00:00:00:00:01")
				.lastIp("192.168.0.11")
				.isActive("active")
				.build());
		RaspberryPi newDevice = raspberryPiRepository.save(RaspberryPi.builder()
				.deviceId(UUID.randomUUID())
				.macAddress("00:00:00:00:00:02")
				.lastIp("192.168.0.12")
				.isActive("active")
				.build());
		Cage cage = cageRepository.save(Cage.builder()
				.id(UUID.randomUUID())
				.facility(facility)
				.name("Before Cage")
				.cageNumber("A-1")
				.status(CageService.STATUS_AVAILABLE)
				.raspberryPiDeviceId(oldDevice.getDeviceId())
				.createdAt(LocalDateTime.now())
				.build());

		mockMvc.perform(patch("/api/facilities/{facilityId}/cages/{cageId}", facility.getId(), cage.getId())
						.header("Authorization", "Bearer manager-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", "Updated Cage",
								"cageNumber", "B-2",
								"raspberryPiDeviceId", newDevice.getDeviceId().toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(cage.getId().toString()))
				.andExpect(jsonPath("$.facilityId").value(facility.getId().toString()))
				.andExpect(jsonPath("$.name").value("Updated Cage"))
				.andExpect(jsonPath("$.cageNumber").value("B-2"))
				.andExpect(jsonPath("$.status").value(CageService.STATUS_AVAILABLE))
				.andExpect(jsonPath("$.raspberryPiDeviceId").value(newDevice.getDeviceId().toString()))
				.andExpect(jsonPath("$.videoUrl", containsString(newDevice.getDeviceId().toString())));

		mockMvc.perform(get("/api/facilities/{facilityId}/cages", facility.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Updated Cage"))
				.andExpect(jsonPath("$[0].cageNumber").value("B-2"))
				.andExpect(jsonPath("$[0].raspberryPiDeviceId").value(newDevice.getDeviceId().toString()));

		mockMvc.perform(post("/api/facilities/{facilityId}/cages", facility.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", "Registered Cage",
								"cageNumber", "C-3",
								"status", CageService.STATUS_AVAILABLE,
								"raspberryPiDeviceId", newDevice.getDeviceId()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Registered Cage"));

		Pet pet = petRepository.save(Pet.builder()
				.id(UUID.randomUUID())
				.owner(owner)
				.name("Choco")
				.breed("Poodle")
				.build());

		MvcResult admissionResult = mockMvc.perform(post("/api/facilities/{facilityId}/admission-sessions", facility.getId())
						.header("Authorization", "Bearer manager-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"ownerEmail", owner.getEmail(),
								"petId", pet.getId(),
								"cageId", cage.getId()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.videoUrl", containsString(newDevice.getDeviceId().toString())))
				.andReturn();
		JsonNode admission = objectMapper.readTree(admissionResult.getResponse().getContentAsString());

		mockMvc.perform(get("/api/owners/me/cages")
						.header("Authorization", "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].cageId").value(cage.getId().toString()))
				.andExpect(jsonPath("$[0].videoUrl", containsString(newDevice.getDeviceId().toString())));

		mockMvc.perform(patch("/api/admission-sessions/{sessionId}/end", admission.get("sessionId").asLong())
						.header("Authorization", "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ENDED"));

		mockMvc.perform(get("/api/facilities/{facilityId}/cages", facility.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value(CageService.STATUS_AVAILABLE));
	}

	@Test
	void facilityManagerCannotUpdateCageOutsideFacility() throws Exception {
		Facility ownFacility = saveFacility("Own Hospital");
		Facility otherFacility = saveFacility("Other Hospital");
		AppUser manager = saveUser("manager@example.com", "Manager", "HOSPITAL", ownFacility.getId());
		saveToken("manager-token", manager);
		Cage otherCage = cageRepository.save(Cage.builder()
				.id(UUID.randomUUID())
				.facility(otherFacility)
				.name("Other Cage")
				.cageNumber("O-1")
				.status(CageService.STATUS_AVAILABLE)
				.createdAt(LocalDateTime.now())
				.build());

		mockMvc.perform(patch("/api/facilities/{facilityId}/cages/{cageId}", ownFacility.getId(), otherCage.getId())
						.header("Authorization", "Bearer manager-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", "Blocked Cage",
								"cageNumber", "B-1"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void facilityCageUpdateRejectsUnknownDeviceAndRequiresName() throws Exception {
		Facility facility = saveFacility("Facility Hospital");
		AppUser manager = saveUser("manager@example.com", "Manager", "FACILITY", facility.getId());
		saveToken("manager-token", manager);
		Cage cage = cageRepository.save(Cage.builder()
				.id(UUID.randomUUID())
				.facility(facility)
				.name("Cage")
				.cageNumber("A-1")
				.status(CageService.STATUS_AVAILABLE)
				.createdAt(LocalDateTime.now())
				.build());

		mockMvc.perform(patch("/api/facilities/{facilityId}/cages/{cageId}", facility.getId(), cage.getId())
						.header("Authorization", "Bearer manager-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", "Updated Cage",
								"raspberryPiDeviceId", UUID.randomUUID().toString()))))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/facilities/{facilityId}/cages/{cageId}", facility.getId(), cage.getId())
						.header("Authorization", "Bearer manager-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("cageNumber", "B-2"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void adminCanUpdateCageAssignmentAndExistingCageViewsStillWork() throws Exception {
		AppUser admin = saveUser("admin@example.com", "Admin", "ADMIN", null);
		AppUser owner = saveUser("owner@example.com", "Owner", "OWNER", null);
		saveToken(ADMIN_TOKEN, admin);
		saveToken(OWNER_TOKEN, owner);

		Facility oldFacility = saveFacility("Old Hospital");
		Facility newFacility = saveFacility("New Hospital");
		RaspberryPi newDevice = raspberryPiRepository.save(RaspberryPi.builder()
				.deviceId(UUID.randomUUID())
				.macAddress("00:11:22:33:44:55")
				.lastIp("192.168.0.10")
				.isActive("active")
				.build());
		Cage cage = cageRepository.save(Cage.builder()
				.id(UUID.randomUUID())
				.facility(oldFacility)
				.name("Assignment Cage")
				.cageNumber("J-1")
				.status(CageService.STATUS_AVAILABLE)
				.createdAt(LocalDateTime.now().minusMinutes(1))
				.build());

		mockMvc.perform(patch("/api/admin/cages/{cageId}/assignment", cage.getId())
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"facilityId", newFacility.getId(),
								"deviceId", newDevice.getDeviceId()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cageId").value(cage.getId().toString()))
				.andExpect(jsonPath("$.facilityId").value(newFacility.getId().toString()))
				.andExpect(jsonPath("$.facilityName").value("New Hospital"))
				.andExpect(jsonPath("$.deviceId").value(newDevice.getDeviceId().toString()))
				.andExpect(jsonPath("$.status").value(CageService.STATUS_AVAILABLE));

		mockMvc.perform(get("/api/admin/cages")
						.header("Authorization", "Bearer " + ADMIN_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].facilityId").value(newFacility.getId().toString()))
				.andExpect(jsonPath("$[0].facilityName").value("New Hospital"))
				.andExpect(jsonPath("$[0].deviceId").value(newDevice.getDeviceId().toString()));

		mockMvc.perform(get("/api/facilities/{facilityId}/cages", newFacility.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(cage.getId().toString()))
				.andExpect(jsonPath("$[0].facilityId").value(newFacility.getId().toString()))
				.andExpect(jsonPath("$[0].raspberryPiDeviceId").value(newDevice.getDeviceId().toString()));

		Pet pet = petRepository.save(Pet.builder()
				.id(UUID.randomUUID())
				.owner(owner)
				.name("Choco")
				.breed("Poodle")
				.build());
		Cage assignedCage = cageRepository.findById(cage.getId()).orElseThrow();
		assignedCage.setStatus(CageService.STATUS_OCCUPIED);
		assignedCage.setUser(owner);
		assignedCage.setCurrentPet(pet);
		assignedCage.setAccessCode("123456");
		cageRepository.save(assignedCage);
		admissionSessionRepository.save(AdmissionSession.builder()
				.owner(owner)
				.pet(pet)
				.cage(assignedCage)
				.accessCode("123456")
				.status("ACTIVE")
				.createdAt(OffsetDateTime.now())
				.build());

		mockMvc.perform(get("/api/owners/me/cages")
						.header("Authorization", "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].cageId").value(cage.getId().toString()))
				.andExpect(jsonPath("$[0].facilityName").value("New Hospital"))
				.andExpect(jsonPath("$[0].videoUrl", containsString(newDevice.getDeviceId().toString())));
	}

	@Test
	void nonAdminCannotUpdateCageAssignment() throws Exception {
		AppUser owner = saveUser("owner@example.com", "Owner", "OWNER", null);
		saveToken(OWNER_TOKEN, owner);
		Facility facility = saveFacility("Hospital");
		Cage cage = cageRepository.save(Cage.builder()
				.id(UUID.randomUUID())
				.facility(facility)
				.name("Cage")
				.cageNumber("C-1")
				.status(CageService.STATUS_AVAILABLE)
				.createdAt(LocalDateTime.now())
				.build());

		mockMvc.perform(patch("/api/admin/cages/{cageId}/assignment", cage.getId())
						.header("Authorization", "Bearer " + OWNER_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void unknownFacilityOrDeviceReturnsNotFound() throws Exception {
		AppUser admin = saveUser("admin@example.com", "Admin", "ADMIN", null);
		saveToken(ADMIN_TOKEN, admin);
		Facility facility = saveFacility("Hospital");
		Cage cage = cageRepository.save(Cage.builder()
				.id(UUID.randomUUID())
				.facility(facility)
				.name("Cage")
				.cageNumber("C-1")
				.status(CageService.STATUS_AVAILABLE)
				.createdAt(LocalDateTime.now())
				.build());

		mockMvc.perform(patch("/api/admin/cages/{cageId}/assignment", cage.getId())
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("facilityId", UUID.randomUUID()))))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/admin/cages/{cageId}/assignment", cage.getId())
						.header("Authorization", "Bearer " + ADMIN_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("deviceId", UUID.randomUUID()))))
				.andExpect(status().isNotFound());
	}

	private AppUser saveUser(String email, String name, String role, UUID hospitalId) {
		return appUserRepository.save(AppUser.builder()
				.id(UUID.randomUUID())
				.email(email)
				.passwordHash("password")
				.name(name)
				.role(role)
				.hospitalId(hospitalId)
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

	private Facility saveFacility(String name) {
		return facilityRepository.save(Facility.builder()
				.id(UUID.randomUUID())
				.name(name)
				.phoneNumber("02-0000-0000")
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
				create table if not exists public.hospitals (
					hospital_id uuid primary key,
					name varchar(20) not null,
					phone varchar(20) not null
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
				create table if not exists public."pets" (
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
					name varchar(100),
					cage_number varchar(50),
					user_id uuid,
					current_pet_id uuid,
					access_code varchar(50),
					status varchar(50) not null,
					device_id uuid,
					created_at timestamp not null
				)
				""");
		jdbcTemplate.execute("""
				create table if not exists public.access_session (
					session_id bigint generated by default as identity primary key,
					user_id uuid not null,
					pet_id uuid not null,
					cage_id uuid not null,
					access_code varchar(50) not null,
					status varchar(50) not null,
					created_at timestamp with time zone not null,
					ended_at timestamp with time zone
				)
				""");
	}

	private void deleteRows() {
		jdbcTemplate.update("delete from public.access_session");
		jdbcTemplate.update("delete from public.cage");
		jdbcTemplate.update("delete from public.\"pets\"");
		jdbcTemplate.update("delete from public.auth_token");
		jdbcTemplate.update("delete from public.raspberrypi");
		jdbcTemplate.update("delete from public.hospitals");
		jdbcTemplate.update("delete from public.users");
	}
}
