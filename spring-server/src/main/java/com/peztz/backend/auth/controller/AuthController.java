package com.peztz.backend.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peztz.backend.auth.dto.AuthLoginRequest;
import com.peztz.backend.auth.dto.AuthLoginResponse;
import com.peztz.backend.auth.dto.AuthSignupRequest;
import com.peztz.backend.auth.dto.AuthUserResponse;
import com.peztz.backend.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "회원가입, 로그인, 내 정보 API")
public class AuthController {

	private final AuthService authService;

	@Operation(
			summary = "회원가입",
			description = "이메일과 비밀번호로 MVP 사용자 계정을 생성합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "회원가입 성공",
							content = @Content(schema = @Schema(implementation = AuthUserResponse.class))),
					@ApiResponse(responseCode = "400", description = "요청 검증 실패 또는 중복 이메일", content = @Content)
			})
	@PostMapping("/signup")
	public ResponseEntity<AuthUserResponse> signup(@Valid @RequestBody AuthSignupRequest request) {
		return ResponseEntity.ok(authService.signup(request));
	}

	@Operation(
			summary = "로그인",
			description = "BCrypt 비밀번호 검증 후 UUID 기반 Bearer 토큰을 발급합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "로그인 성공",
							content = @Content(schema = @Schema(implementation = AuthLoginResponse.class))),
					@ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치", content = @Content)
			})
	@PostMapping("/login")
	public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@Operation(
			summary = "내 정보 조회",
			description = "Authorization: Bearer {token} 헤더로 현재 사용자를 조회합니다.",
			responses = {
					@ApiResponse(responseCode = "200", description = "내 정보 조회 성공",
							content = @Content(schema = @Schema(implementation = AuthUserResponse.class))),
					@ApiResponse(responseCode = "401", description = "토큰 없음, 만료, 또는 유효하지 않은 토큰", content = @Content)
			})
	@GetMapping("/me")
	public ResponseEntity<AuthUserResponse> me(
			@Parameter(description = "Bearer access token", example = "Bearer sample-token", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(authService.me(authorization));
	}
}
