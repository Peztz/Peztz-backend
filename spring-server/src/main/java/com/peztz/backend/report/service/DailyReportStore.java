package com.peztz.backend.report.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.peztz.backend.report.entity.DailyReport;

public interface DailyReportStore {

	Optional<DailyReportClaim> tryClaim(
			UUID petId,
			LocalDate reportDate,
			DailyReportStatistics statistics,
			OffsetDateTime now,
			OffsetDateTime retryBefore,
			OffsetDateTime staleBefore);

	Optional<DailyReport> findByPetAndDate(UUID petId, LocalDate reportDate);

	Optional<DailyReport> findById(UUID reportId);

	boolean complete(
			DailyReportClaim claim,
			String status,
			Map<String, Object> content,
			String modelName,
			String errorMessage,
			OffsetDateTime generatedAt);
}
