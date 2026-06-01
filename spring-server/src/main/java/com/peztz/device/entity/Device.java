package com.peztz.device.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {

	private Long id;
	private String macAddress;
	private String ipAddress;
	private Integer streamPort;
	private LocalDateTime lastSeen;
}
