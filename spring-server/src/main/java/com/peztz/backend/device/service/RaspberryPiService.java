package com.peztz.backend.device.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
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
		String macAddress = request.getMacAddress().trim().toUpperCase(Locale.ROOT);
		String validatedIp = validateAndNormalizeIp(request.getLastIp());
		RaspberryPi raspberryPi = raspberryPiRepository.findByMacAddress(macAddress)
				.orElseGet(() -> RaspberryPi.builder()
						.deviceId(UUID.randomUUID())
						.macAddress(macAddress)
						.build());

		raspberryPi.setLastIp(validatedIp);
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
		if (!StringUtils.hasText(macAddress)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "macAddress is empty");
		}
		RaspberryPi raspberryPi = raspberryPiRepository.findByMacAddress(macAddress.trim().toUpperCase(Locale.ROOT))
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

	private String validateAndNormalizeIp(String rawIp) {
		if (!StringUtils.hasText(rawIp)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Raspberry Pi lastIp is empty");
		}
		String ip = rawIp.trim();
		String[] parts = ip.split("\\.", -1);
		if (parts.length != 4) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Raspberry Pi lastIp must be an IPv4 address");
		}

		int[] octets = new int[4];
		for (int i = 0; i < parts.length; i++) {
			if (!parts[i].matches("\\d{1,3}")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Raspberry Pi lastIp must be an IPv4 address");
			}
			octets[i] = Integer.parseInt(parts[i]);
			if (octets[i] > 255) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Raspberry Pi lastIp must be an IPv4 address");
			}
		}

		boolean allowed = octets[0] == 10
				|| (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
				|| (octets[0] == 192 && octets[1] == 168)
				|| (octets[0] == 100 && octets[1] >= 64 && octets[1] <= 127);
		if (!allowed) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Raspberry Pi lastIp must be a private or Tailscale IPv4 address");
		}
		return String.format("%d.%d.%d.%d", octets[0], octets[1], octets[2], octets[3]);
	}
}
