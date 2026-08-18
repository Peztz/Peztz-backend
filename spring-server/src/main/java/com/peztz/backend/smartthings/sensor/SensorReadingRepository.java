package com.peztz.backend.smartthings.sensor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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

	List<SensorReading> findByCageIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
			UUID cageId,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable);
}
