package com.peztz.backend.facility.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.facility.dto.FacilityAdmissionSessionCreateRequest;
import com.peztz.backend.facility.dto.FacilityAdmissionSessionDetailResponse;
import com.peztz.backend.facility.dto.FacilityAdmissionSessionResponse;
import com.peztz.backend.facility.dto.FacilityCageUpdateRequest;
import com.peztz.backend.facility.dto.FacilityOwnerPetResponse;
import com.peztz.backend.facility.dto.FacilityRequest;
import com.peztz.backend.facility.dto.FacilityResponse;
import com.peztz.backend.cage.dto.CageResponse;
import com.peztz.backend.facility.service.FacilityAdmissionService;
import com.peztz.backend.facility.service.FacilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
@Tag(name = "시설", description = "시설 등록, 조회 및 시설 입실 관리 API")
public class FacilityController {

	private final FacilityService facilityService;
	private final FacilityAdmissionService facilityAdmissionService;

	@Operation(summary = "시설 목록 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = FacilityResponse.class))))
	})
	@GetMapping
	public ResponseEntity<List<FacilityResponse>> findAll() {
		return ResponseEntity.ok(facilityService.findAll());
	}

	@Operation(summary = "시설 생성", responses = {
			@ApiResponse(responseCode = "200", description = "생성 성공",
					content = @Content(schema = @Schema(implementation = FacilityResponse.class))),
			@ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content)
	})
	@PostMapping
	public ResponseEntity<FacilityResponse> create(@Valid @RequestBody FacilityRequest request) {
		return ResponseEntity.ok(facilityService.create(request));
	}

	@Operation(summary = "시설 단건 조회", responses = {
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(schema = @Schema(implementation = FacilityResponse.class))),
			@ApiResponse(responseCode = "404", description = "시설을 찾을 수 없음", content = @Content)
	})
	@GetMapping("/{facilityId}")
	public ResponseEntity<FacilityResponse> findById(
			@Parameter(description = "시설 ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId) {
		return ResponseEntity.ok(facilityService.findById(facilityId));
	}

	@Operation(
			summary = "시설 입실용 견주 반려동물 조회",
			description = "시설 담당자가 이메일로 견주를 검색하고 해당 견주의 반려동물 목록을 조회합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(array = @ArraySchema(schema = @Schema(implementation = FacilityOwnerPetResponse.class)))),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
					@ApiResponse(responseCode = "403", description = "시설 관리자 권한 필요", content = @Content),
					@ApiResponse(responseCode = "404", description = "시설 또는 견주를 찾을 수 없음", content = @Content)
			})
	@GetMapping("/{facilityId}/owners/pets")
	public ResponseEntity<List<FacilityOwnerPetResponse>> findOwnerPets(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "시설 ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Parameter(description = "견주 이메일", example = "test@naver.com", required = true)
			@RequestParam String email) {
		return ResponseEntity.ok(facilityAdmissionService.findOwnerPets(authorization, facilityId, email));
	}

	@Operation(
			summary = "시설 입실 세션 생성",
			description = "시설 담당자가 견주의 반려동물을 사용 가능한 케이지에 입실시키고 접근 코드를 발급합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "생성 성공",
							content = @Content(schema = @Schema(implementation = FacilityAdmissionSessionResponse.class))),
					@ApiResponse(responseCode = "400", description = "잘못된 견주 반려동물, 케이지 또는 케이지 상태", content = @Content),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
					@ApiResponse(responseCode = "403", description = "시설 관리자 권한 필요", content = @Content),
					@ApiResponse(responseCode = "404", description = "시설, 견주, 반려동물 또는 케이지를 찾을 수 없음", content = @Content)
			})
	@PostMapping("/{facilityId}/admission-sessions")
	public ResponseEntity<FacilityAdmissionSessionResponse> createAdmissionSession(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "시설 ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Valid @RequestBody FacilityAdmissionSessionCreateRequest request) {
		return ResponseEntity.ok(facilityAdmissionService.createAdmissionSession(authorization, facilityId, request));
	}

	@Operation(
			summary = "시설 케이지 수정",
			description = "시설 담당자는 케이지 이름, 케이지 번호 및 연결된 라즈베리파이만 수정할 수 있습니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "수정 성공",
							content = @Content(schema = @Schema(implementation = CageResponse.class))),
					@ApiResponse(responseCode = "400", description = "잘못된 요청 또는 장치 ID", content = @Content),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
					@ApiResponse(responseCode = "403", description = "시설 관리자 권한이 없거나 시설 외부 케이지에 접근함", content = @Content),
					@ApiResponse(responseCode = "404", description = "시설, 케이지 또는 라즈베리파이를 찾을 수 없음", content = @Content)
			})
	@PatchMapping("/{facilityId}/cages/{cageId}")
	public ResponseEntity<CageResponse> updateCage(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "시설 ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Parameter(description = "케이지 ID", example = "d69fc7ff-481c-4305-b81c-551955a1ce23")
			@PathVariable UUID cageId,
			@Valid @RequestBody FacilityCageUpdateRequest request) {
		return ResponseEntity.ok(facilityAdmissionService.updateCage(authorization, facilityId, cageId, request));
	}

	@Operation(
			summary = "시설 입실 세션 목록 조회",
			description = "시설 담당자가 이 시설의 케이지에 연결된 세션을 조회합니다. 상태를 생략하면 기본값으로 ACTIVE를 사용합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "조회 성공",
							content = @Content(array = @ArraySchema(schema = @Schema(implementation = FacilityAdmissionSessionDetailResponse.class)))),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
					@ApiResponse(responseCode = "403", description = "시설 관리자 권한 필요", content = @Content),
					@ApiResponse(responseCode = "404", description = "시설을 찾을 수 없음", content = @Content)
			})
	@GetMapping("/{facilityId}/admission-sessions")
	public ResponseEntity<List<FacilityAdmissionSessionDetailResponse>> findAdmissionSessions(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "시설 ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Parameter(description = "세션 상태입니다. 생략하면 ACTIVE를 기본값으로 사용합니다.", example = "ACTIVE")
			@RequestParam(required = false) String status) {
		return ResponseEntity.ok(facilityAdmissionService.findAdmissionSessions(authorization, facilityId, status));
	}

	@Operation(
			summary = "시설 입실 세션 종료",
			description = "시설 담당자가 이 시설의 케이지에 연결된 세션을 종료합니다. 기존 /api/admission-sessions/{sessionId}/end는 견주 범위이며, 이 API는 시설 케이지 범위입니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "종료 성공",
							content = @Content(schema = @Schema(implementation = FacilityAdmissionSessionDetailResponse.class))),
					@ApiResponse(responseCode = "400", description = "ACTIVE 상태가 아닌 세션", content = @Content),
					@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
					@ApiResponse(responseCode = "403", description = "시설 관리자 권한 필요", content = @Content),
					@ApiResponse(responseCode = "404", description = "시설 또는 입실 세션을 찾을 수 없음", content = @Content)
			})
	@PatchMapping("/{facilityId}/admission-sessions/{sessionId}/end")
	public ResponseEntity<FacilityAdmissionSessionDetailResponse> endAdmissionSession(
			@Parameter(description = "Bearer 인증 토큰", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Parameter(description = "시설 ID", example = "11111111-1111-1111-1111-111111111111")
			@PathVariable UUID facilityId,
			@Parameter(description = "세션 ID", example = "1000000003")
			@PathVariable Long sessionId) {
		return ResponseEntity.ok(facilityAdmissionService.endAdmissionSession(authorization, facilityId, sessionId));
	}
}
