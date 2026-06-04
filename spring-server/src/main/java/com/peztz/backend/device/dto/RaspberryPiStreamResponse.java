package com.peztz.backend.device.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RaspberryPiStreamResponse {

	private UUID deviceId;
	private String macAddress;
	private String lastIp;
	private String streamUrl;
}
