package com.peztz.backend.smartthings.device;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.peztz.backend.smartthings.dto.SmartThingsDeviceRegistrationRequest;
import com.peztz.backend.smartthings.dto.SmartThingsMappedDeviceResponse;
import com.peztz.backend.smartthings.exception.SmartThingsApiException;
import com.peztz.backend.smartthings.sensor.SmartThingsSensorSyncService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmartThingsDeviceRegistrationService {

	private static final Logger log = LoggerFactory.getLogger(SmartThingsDeviceRegistrationService.class);

	private final SmartThingsDeviceService deviceService;
	private final SmartThingsDeviceRegistrationValidator registrationValidator;
	private final SmartThingsSensorSyncService syncService;

	public SmartThingsMappedDeviceResponse register(
			String authorization,
			UUID cageId,
			SmartThingsDeviceRegistrationRequest request) {
		deviceService.requireCageAccess(authorization, cageId);
		String deviceId = request.deviceId().trim();
		SmartThingsDeviceType deviceType = registrationValidator.validate(deviceId, request.deviceType());
		SmartThingsMappedDeviceResponse registered = deviceService.registerValidated(
				authorization, cageId, request, deviceType);

		try {
			syncService.sync(registered.mappingId(), "REGISTRATION");
		} catch (SmartThingsApiException exception) {
			log.warn("Initial SmartThings sync failed mappingId={} code={}",
					registered.mappingId(), exception.getCode());
		}
		return deviceService.findByMappingId(authorization, registered.mappingId());
	}
}
