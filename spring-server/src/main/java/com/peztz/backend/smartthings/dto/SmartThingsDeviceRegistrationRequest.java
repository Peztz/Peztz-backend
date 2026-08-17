package com.peztz.backend.smartthings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SmartThingsDeviceRegistrationRequest(
		@NotBlank @Size(max = 100)
		@Schema(example = "f629bdb6-304d-42db-8954-428621a80fae")
		String deviceId,

		@NotBlank @Size(max = 50)
		@Schema(example = "ILLUMINANCE", allowableValues = {"CONTACT", "ILLUMINANCE", "TEMPERATURE_HUMIDITY"})
		String deviceType,

		@Size(max = 100)
		@Schema(example = "Cage A illuminance sensor")
		String label) {
}
