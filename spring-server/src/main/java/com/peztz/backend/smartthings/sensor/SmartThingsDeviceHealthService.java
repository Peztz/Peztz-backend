package com.peztz.backend.smartthings.sensor;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.peztz.backend.smartthings.device.SmartThingsDeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmartThingsDeviceHealthService {

	private final SmartThingsDeviceRepository deviceRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markOffline(UUID mappingId) {
		deviceRepository.findById(mappingId).ifPresent(device -> device.setOnline(false));
	}
}
