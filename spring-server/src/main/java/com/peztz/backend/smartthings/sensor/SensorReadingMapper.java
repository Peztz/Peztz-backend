package com.peztz.backend.smartthings.sensor;

import org.springframework.stereotype.Component;

import com.peztz.backend.smartthings.dto.SensorReadingResponse;

@Component
public class SensorReadingMapper {

	public SensorReadingResponse toResponse(SensorReading reading) {
		return new SensorReadingResponse(
				reading.getId(),
				reading.getCage().getId(),
				reading.getSession() == null ? null : reading.getSession().getId(),
				reading.getDevice().getSmartThingsDeviceId(),
				reading.getDevice().getDeviceType(),
				reading.getCapability(),
				reading.getAttribute(),
				reading.getNumericValue(),
				reading.getStringValue(),
				reading.getUnit(),
				reading.getMeasuredAt(),
				reading.getReceivedAt(),
				reading.getSource());
	}
}
