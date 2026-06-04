package com.peztz.backend.device.service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.peztz.backend.device.dto.RaspberryPiRegisterRequest;
import com.peztz.backend.device.dto.RaspberryPiResponse;
import com.peztz.backend.device.entity.RaspberryPi;
import com.peztz.backend.device.repository.RaspberryPiRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RaspberryPiService {

	private static final String ACTIVE_STATUS = "active";

	private final RaspberryPiRepository raspberryPiRepository;

	@Transactional
	public RaspberryPiResponse register(RaspberryPiRegisterRequest request) {
		RaspberryPi raspberryPi = raspberryPiRepository.findByMacAddress(request.getMacAddress())
				.orElseGet(() -> RaspberryPi.builder()
						.deviceId(UUID.randomUUID())
						.macAddress(request.getMacAddress())
						.build());

		raspberryPi.setLastIp(request.getLastIp());
		raspberryPi.setIsActive(ACTIVE_STATUS);
		raspberryPi.setLastPing(LocalTime.now().withNano(0));

		return toResponse(raspberryPiRepository.save(raspberryPi));
	}

	@Transactional(readOnly = true)
	public List<RaspberryPiResponse> findAll() {
		return raspberryPiRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	private RaspberryPiResponse toResponse(RaspberryPi raspberryPi) {
		return new RaspberryPiResponse(
				raspberryPi.getDeviceId(),
				raspberryPi.getMacAddress(),
				raspberryPi.getLastIp(),
				raspberryPi.getIsActive(),
				raspberryPi.getLastPing());
	}
}
