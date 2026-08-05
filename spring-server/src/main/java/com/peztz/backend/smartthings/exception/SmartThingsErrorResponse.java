package com.peztz.backend.smartthings.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스마트싱스 API 오류 응답")
public record SmartThingsErrorResponse(
		String code,
		String message) {
}
