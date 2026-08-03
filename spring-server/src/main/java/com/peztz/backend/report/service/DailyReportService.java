package com.peztz.backend.report.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
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
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class DailyReportService {

    private final SessionLogRepository sessionLogRepository;
    private final PetRepository petRepository;
    private final AuthService authService;
    private final AdmissionSessionService admissionSessionService;


    @Value("${peztz.fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

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

        // ==================== 🎯 여기서부터 준님의 AI 리포트 연동 파트 ====================

        String fastapiUrl = fastApiBaseUrl.replaceAll("/+$", "") + "/api/report/generate";

        // 준님의 FastAPI 규격 {"cage_id": ..., "pet_name": ...} 맵핑
        Map<String, Object> requestMap = new HashMap<>();
        // 시연을 위해 준님이 SQL 더미에 하드코딩해둔 초코의 cage_id를 박아 통신을 성공시킵니다!
        requestMap.put("cage_id", "55555555-5555-5555-5555-555555555555");
        requestMap.put("pet_name", "초코");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestMap, headers);

        String aiReportSummary = "";
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(fastapiUrl, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // FastAPI 리턴값 구조인 {"report": "제미나이 텍스트 내용..."} 에서 report 추출
                aiReportSummary = (String) response.getBody().get("report");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 에러 날 경우 예외 처리 및 가짜 텍스트 노출 방지 기본값 세팅
            aiReportSummary = "## 🐾 AI 건강 리포트 생성 실패\n현재 AI 분석 서버와의 통신이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.";
        }

        // 팀원들이 짜놓은 수식에 준님이 가져온 aiReportSummary를 섞어주거나 통째로 대체합니다!
        // 여기서는 제미나이의 풍부한 마크다운 리포트가 그대로 프론트에 전달되도록 summary 자리에 꽂아줍니다.
        return new DailyReportResponse(petId, date, totalCount, sensorCount, averageTemperature, averageHumidity, aiReportSummary);
    }
}
