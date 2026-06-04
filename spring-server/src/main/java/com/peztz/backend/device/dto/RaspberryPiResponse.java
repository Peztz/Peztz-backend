package com.peztz.backend.device.dto;

import java.time.LocalTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RaspberryPiResponse {

	private UUID deviceId;
	private String macAddress;
	private String lastIp;
	private String isActive;
	private LocalTime lastPing;
}
