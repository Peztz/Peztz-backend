package com.peztz.backend.log.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.service.AdmissionSessionService;
import com.peztz.backend.log.dto.SessionLogRequest;
import com.peztz.backend.log.dto.SessionLogResponse;
import com.peztz.backend.log.entity.SessionLog;
import com.peztz.backend.log.repository.SessionLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionLogService {

	private final SessionLogRepository sessionLogRepository;
	private final AdmissionSessionService admissionSessionService;

	@Transactional(readOnly = true)
	public List<SessionLogResponse> findBySession(String authorization, Long sessionId) {
		admissionSessionService.getOwnedSession(authorization, sessionId);
		return sessionLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public SessionLogResponse create(String authorization, Long sessionId, SessionLogRequest request) {
		AdmissionSession session = admissionSessionService.getOwnedSession(authorization, sessionId);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		if (request.message() != null) {
			data.put("message", request.message());
		}
		if (request.temperature() != null) {
			data.put("temperature", request.temperature());
		}
		if (request.humidity() != null) {
			data.put("humidity", request.humidity());
		}
		SessionLog log = SessionLog.builder()
				.session(session)
				.videoId(null)
				.type(request.type().trim().toUpperCase(Locale.ROOT))
				.data(data)
				.build();
		return toResponse(sessionLogRepository.save(log));
	}

	public SessionLogResponse toResponse(SessionLog log) {
		return new SessionLogResponse(
				log.getId(),
				log.getType(),
				log.getMessage(),
				log.getTemperature(),
				log.getHumidity(),
				log.getCreatedAtAsLocalDateTime());
	}
}
