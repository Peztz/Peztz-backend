package com.peztz.device.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceResponse {

	private Long id;
	private String macAddress;
	private String ipAddress;
	private Integer streamPort;
}
