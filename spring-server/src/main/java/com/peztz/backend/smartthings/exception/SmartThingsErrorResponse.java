package com.peztz.backend.smartthings.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SmartThings API error response")
public record SmartThingsErrorResponse(
		String code,
		String message) {
}
