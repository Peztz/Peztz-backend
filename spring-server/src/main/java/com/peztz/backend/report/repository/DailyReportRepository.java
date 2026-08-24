package com.peztz.backend.report.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.report.entity.DailyReport;

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

	Optional<DailyReport> findByPetIdAndReportDate(UUID petId, LocalDate reportDate);
}
