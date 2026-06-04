package com.peztz.backend.device.service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.device.dto.RaspberryPiRegisterRequest;
import com.peztz.backend.device.dto.RaspberryPiResponse;
import com.peztz.backend.device.dto.RaspberryPiStreamResponse;
import com.peztz.backend.device.entity.RaspberryPi;
import com.peztz.backend.device.repository.RaspberryPiRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RaspberryPiService {

	private static final String ACTIVE_STATUS = "active";

	private final RaspberryPiRepository raspberryPiRepository;

	@Value("${raspberrypi.stream.port:8001}")
	private int streamPort;

	@Value("${raspberrypi.stream.path:/video_feed}")
	private String streamPath;

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

	@Transactional(readOnly = true)
	public RaspberryPiStreamResponse getStreamUrl(UUID deviceId) {
		RaspberryPi raspberryPi = raspberryPiRepository.findById(deviceId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Raspberry Pi not found"));

		return toStreamResponse(raspberryPi);
	}

	@Transactional(readOnly = true)
	public RaspberryPiStreamResponse getStreamUrlByMacAddress(String macAddress) {
		RaspberryPi raspberryPi = raspberryPiRepository.findByMacAddress(macAddress)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Raspberry Pi not found"));

		return toStreamResponse(raspberryPi);
	}

	private RaspberryPiResponse toResponse(RaspberryPi raspberryPi) {
		return new RaspberryPiResponse(
				raspberryPi.getDeviceId(),
				raspberryPi.getMacAddress(),
				raspberryPi.getLastIp(),
				raspberryPi.getIsActive(),
				raspberryPi.getLastPing());
	}

	private RaspberryPiStreamResponse toStreamResponse(RaspberryPi raspberryPi) {
		if (!StringUtils.hasText(raspberryPi.getLastIp())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Raspberry Pi lastIp is empty");
		}

		String lastIp = raspberryPi.getLastIp().trim();
		String normalizedStreamPath = streamPath.startsWith("/") ? streamPath : "/" + streamPath;
		String streamUrl = "http://" + lastIp + ":" + streamPort + normalizedStreamPath;

		return new RaspberryPiStreamResponse(
				raspberryPi.getDeviceId(),
				raspberryPi.getMacAddress(),
				lastIp,
				streamUrl);
	}
}
