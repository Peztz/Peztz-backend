package com.peztz.backend.report.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.peztz.backend.log.repository.SessionLogRepository;
import com.peztz.backend.pet.entity.Pet;
import com.peztz.backend.pet.repository.PetRepository;
import com.peztz.backend.smartthings.sensor.SensorReadingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyReportSourceService {

	private final SessionLogRepository sessionLogRepository;
	private final SensorReadingRepository sensorReadingRepository;
	private final PetRepository petRepository;

	@Transactional(readOnly = true)
	public DailyReportSource load(
			UUID petId,
			OffsetDateTime start,
			OffsetDateTime endExclusive) {
		Pet pet = petRepository.findById(petId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

		List<DailyReportSource.LogObservation> logs = sessionLogRepository
				.findByPetIdAndCreatedAtRange(petId, start, endExclusive).stream()
				.map(log -> new DailyReportSource.LogObservation(
						log.getType(),
						log.getCreatedAt(),
						log.getEventDurationSeconds(),
						log.getData() == null ? new HashMap<>() : new HashMap<>(log.getData())))
				.toList();

		List<DailyReportSource.SensorMeasurement> sensorMeasurements = sensorReadingRepository
				.findReportMeasurementsByPetIdAndMeasuredAtRange(petId, start, endExclusive).stream()
				.map(reading -> new DailyReportSource.SensorMeasurement(
						reading.getAttribute(),
						reading.getNumericValue().doubleValue(),
						reading.getUnit(),
						reading.getMeasuredAt()))
				.toList();

		return new DailyReportSource(
				new DailyReportSource.PetProfile(
						pet.getId(), pet.getName(), pet.getBreed(), pet.getBirthDate()),
				logs,
				sensorMeasurements);
	}
}
