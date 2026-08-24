package com.peztz.backend.report.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.peztz.backend.report.dto.DailyReportResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "peztz.report.scheduling-enabled", havingValue = "true")
public class DailyReportGenerationJob {

	private static final Logger log = LoggerFactory.getLogger(DailyReportGenerationJob.class);

	private final DailyReportService dailyReportService;

	@Value("${peztz.report.time-zone:Asia/Seoul}")
	private String reportTimeZone;

	@Scheduled(
			cron = "${peztz.report.schedule-cron:0 10 0 * * *}",
			zone = "${peztz.report.time-zone:Asia/Seoul}")
	public void generatePreviousDayReports() {
		LocalDate reportDate = LocalDate.now(ZoneId.of(reportTimeZone)).minusDays(1);
		for (UUID petId : dailyReportService.findScheduledPetIds(reportDate)) {
			try {
				DailyReportResponse report = dailyReportService.generateScheduledReport(petId, reportDate);
				log.info("Daily report generation completed pet={} date={} status={}",
						maskedPetId(petId), reportDate, report.status());
			} catch (RuntimeException exception) {
				log.warn("Daily report generation failed pet={} date={} reason={}",
						maskedPetId(petId), reportDate, exception.getMessage());
			}
		}
	}

	private String maskedPetId(UUID petId) {
		String value = petId.toString();
		return value.substring(0, 8) + "...";
	}
}
