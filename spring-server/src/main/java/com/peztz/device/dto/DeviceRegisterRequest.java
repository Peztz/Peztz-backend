package com.peztz.device.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeviceRegisterRequest {

	@NotBlank
	private String mac;

	@NotBlank
	private String ip;

	@NotNull
	@Min(1)
	@Max(65535)
	private Integer streamPort;
}
