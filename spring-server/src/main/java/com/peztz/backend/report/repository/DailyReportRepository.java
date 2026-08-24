package com.peztz.backend.report.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.peztz.backend.report.entity.DailyReport;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

	@EntityGraph(attributePaths = "pet")
	Optional<DailyReport> findByPetIdAndReportDate(UUID petId, LocalDate reportDate);

	@EntityGraph(attributePaths = "pet")
	@Query("select report from DailyReport report where report.id = :reportId")
	@QueryHints(@QueryHint(name = "jakarta.persistence.cache.storeMode", value = "REFRESH"))
	Optional<DailyReport> findWithPetById(@Param("reportId") UUID reportId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "pet")
	@Query("select report from DailyReport report where report.id = :reportId")
	Optional<DailyReport> findWithPetByIdForUpdate(@Param("reportId") UUID reportId);
}
