package com.peztz.backend.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RaspberryPiRegisterRequest {

	@NotBlank
	private String macAddress;

	@NotBlank
	private String lastIp;
}
