package com.peztz.backend.report.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.admission.service.AdmissionSessionService;
import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.auth.service.AuthService;
import com.peztz.backend.log.entity.SessionLog;
import com.peztz.backend.log.repository.SessionLogRepository;
import com.peztz.backend.pet.repository.PetRepository;
import com.peztz.backend.report.dto.DailyReportResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyReportService {

	private final SessionLogRepository sessionLogRepository;
	private final PetRepository petRepository;
	private final AuthService authService;
	private final AdmissionSessionService admissionSessionService;

	@Transactional(readOnly = true)
	public DailyReportResponse getByPet(String authorization, UUID petId, LocalDate date) {
		AppUser owner = authService.requireUser(authorization);
		if (petRepository.findByIdAndOwnerId(petId, owner.getId()).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found");
		}

		OffsetDateTime start = date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
		OffsetDateTime end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime().minusNanos(1);
		return buildReport(petId, date, sessionLogRepository.findByPetIdAndCreatedAtBetween(petId, start, end));
	}

	@Transactional(readOnly = true)
	public DailyReportResponse getBySession(String authorization, Long sessionId, LocalDate date) {
		AdmissionSession session = admissionSessionService.getOwnedSession(authorization, sessionId);
		OffsetDateTime start = date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
		OffsetDateTime end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime().minusNanos(1);
		return buildReport(
				session.getPet().getId(),
				date,
				sessionLogRepository.findBySessionIdAndCreatedAtBetweenOrderByCreatedAtAsc(sessionId, start, end));
	}

	private DailyReportResponse buildReport(UUID petId, LocalDate date, List<SessionLog> logs) {
		long totalCount = logs.size();
		long sensorCount = logs.stream()
				.filter(log -> "SENSOR".equals(log.getType()))
				.count();
		Double averageTemperature = logs.stream()
				.map(SessionLog::getTemperature)
				.filter(value -> value != null)
				.mapToDouble(Double::doubleValue)
				.average()
				.stream()
				.map(value -> Math.round(value * 10.0) / 10.0)
				.boxed()
				.findFirst()
				.orElse(null);
		Double averageHumidity = logs.stream()
				.map(SessionLog::getHumidity)
				.filter(value -> value != null)
				.mapToDouble(Double::doubleValue)
				.average()
				.stream()
				.map(value -> Math.round(value * 10.0) / 10.0)
				.boxed()
				.findFirst()
				.orElse(null);
		String summary = "오늘은 총 " + totalCount + "개의 기록이 등록되었습니다.";

		return new DailyReportResponse(petId, date, totalCount, sensorCount, averageTemperature, averageHumidity, summary);
	}
}
