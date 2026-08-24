package com.peztz.backend.smartthings.sensor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

	boolean existsByDeviceIdAndCapabilityAndAttributeAndMeasuredAt(
			UUID deviceId,
			String capability,
			String attribute,
			OffsetDateTime measuredAt);

	Optional<SensorReading> findFirstByDeviceIdAndCageIdAndCapabilityAndAttributeOrderByMeasuredAtDesc(
			UUID deviceId,
			UUID cageId,
			String capability,
			String attribute);

	List<SensorReading> findByCageIdOrderByMeasuredAtDesc(UUID cageId, Pageable pageable);

	List<SensorReading> findByCageIdAndDeviceActiveTrueOrderByMeasuredAtDesc(
			UUID cageId,
			Pageable pageable);

	List<SensorReading> findByCageIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
			UUID cageId,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable);

	@Query("""
			select reading
			from SensorReading reading
			where reading.session.pet.id = :petId
			  and reading.measuredAt >= :start
			  and reading.measuredAt < :endExclusive
			  and lower(reading.attribute) in ('temperature', 'humidity')
			  and reading.numericValue is not null
			order by reading.measuredAt asc, reading.id asc
			""")
	List<SensorReading> findReportMeasurementsByPetIdAndMeasuredAtRange(
			@Param("petId") UUID petId,
			@Param("start") OffsetDateTime start,
			@Param("endExclusive") OffsetDateTime endExclusive);
}
