package com.peztz.backend.smartthings.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SmartThingsExceptionHandler {

	@ExceptionHandler(SmartThingsApiException.class)
	public ResponseEntity<SmartThingsErrorResponse> handleSmartThingsApiException(SmartThingsApiException exception) {
		return ResponseEntity.status(exception.getStatus())
				.body(new SmartThingsErrorResponse(exception.getCode(), exception.getMessage()));
	}
}
