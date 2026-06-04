package com.peztz.backend.device.entity;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "raspberrypi", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaspberryPi {

	@Id
	@Column(name = "device_id")
	private UUID deviceId;

	@Column(name = "mac_address")
	private String macAddress;

	@Column(name = "last_ip")
	private String lastIp;

	@Column(name = "is_active")
	private String isActive;

	@Column(name = "last_ping")
	private LocalTime lastPing;
}
