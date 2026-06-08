package com.peztz.backend.cage.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VideoUrlService {

	@Value("${FASTAPI_BASE_URL:http://34.50.7.78:8000}")
	private String fastApiBaseUrl;

	public String buildVideoUrl(UUID raspberryPiDeviceId) {
		if (raspberryPiDeviceId == null) {
			return null;
		}

		String baseUrl = fastApiBaseUrl.endsWith("/")
				? fastApiBaseUrl.substring(0, fastApiBaseUrl.length() - 1)
				: fastApiBaseUrl;
		return baseUrl + "/video/" + raspberryPiDeviceId;
	}
}
