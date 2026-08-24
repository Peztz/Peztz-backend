package com.peztz.backend.report.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.peztz.backend.report.entity.DailyReport;
import com.peztz.backend.report.repository.DailyReportRepository;
import com.peztz.backend.report.repository.PostgresDailyReportClaimRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionalDailyReportStore implements DailyReportStore {

	private final DailyReportRepository dailyReportRepository;
	private final PostgresDailyReportClaimRepository claimRepository;

	@Override
	@Transactional
	public Optional<DailyReportClaim> tryClaim(
			UUID petId,
			LocalDate reportDate,
			DailyReportStatistics statistics,
			OffsetDateTime now,
			OffsetDateTime retryBefore,
			OffsetDateTime staleBefore) {
		return claimRepository.tryClaim(petId, reportDate, statistics, now, retryBefore, staleBefore);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DailyReport> findByPetAndDate(UUID petId, LocalDate reportDate) {
		return dailyReportRepository.findByPetIdAndReportDate(petId, reportDate);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DailyReport> findById(UUID reportId) {
		return dailyReportRepository.findWithPetById(reportId);
	}

	@Override
	@Transactional
	public boolean complete(
			DailyReportClaim claim,
			String status,
			Map<String, Object> content,
			String modelName,
			String errorMessage,
			OffsetDateTime generatedAt) {
		DailyReport report = dailyReportRepository.findWithPetByIdForUpdate(claim.reportId())
				.orElseThrow(() -> new IllegalStateException("Claimed daily report was not found"));
		if (!DailyReportService.STATUS_GENERATING.equals(report.getStatus())
				|| !claim.generationToken().equals(report.getGenerationToken())) {
			return false;
		}

		report.setStatus(status);
		report.setGenerationToken(null);
		report.setContent(content);
		report.setModelName(modelName);
		report.setErrorMessage(errorMessage);
		report.setGeneratedAt(generatedAt);
		dailyReportRepository.saveAndFlush(report);
		return true;
	}
}
