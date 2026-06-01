package com.peztz.device.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.peztz.device.dto.DeviceRegisterRequest;
import com.peztz.device.dto.DeviceResponse;
import com.peztz.device.entity.Device;
import com.peztz.device.repository.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceService {

	private final DeviceRepository deviceRepository;

	public void register(DeviceRegisterRequest request) {
		Device device = deviceRepository.findByMacAddress(request.getMac())
				.orElseGet(() -> Device.builder()
						.macAddress(request.getMac())
						.build());

		device.setIpAddress(request.getIp());
		device.setStreamPort(request.getStreamPort());
		device.setLastSeen(LocalDateTime.now());

		deviceRepository.save(device);
	}

	public List<DeviceResponse> findAll() {
		return deviceRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	private DeviceResponse toResponse(Device device) {
		return new DeviceResponse(
				device.getId(),
				device.getMacAddress(),
				device.getIpAddress(),
				device.getStreamPort());
	}
}
