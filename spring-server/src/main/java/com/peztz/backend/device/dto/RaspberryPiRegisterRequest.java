package com.peztz.backend.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "라즈베리파이 등록 또는 갱신 요청")
public class RaspberryPiRegisterRequest {

	@NotBlank
	@Schema(description = "라즈베리파이 MAC 주소", example = "88:A2:9E:3D:02:BD")
	private String macAddress;

	@NotBlank
	@Schema(description = "마지막으로 등록된 라즈베리파이 IP 주소", example = "192.168.150.142")
	private String lastIp;
}
