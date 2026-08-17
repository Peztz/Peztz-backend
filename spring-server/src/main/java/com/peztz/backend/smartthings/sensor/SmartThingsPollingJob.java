package com.peztz.backend.smartthings.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.peztz.backend.smartthings.device.SmartThingsDevice;
import com.peztz.backend.smartthings.device.SmartThingsDeviceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "smartthings.polling-enabled", havingValue = "true")
public class SmartThingsPollingJob {

	private static final Logger log = LoggerFactory.getLogger(SmartThingsPollingJob.class);

	private final SmartThingsDeviceRepository deviceRepository;
	private final SmartThingsSensorSyncService syncService;

	@Scheduled(fixedDelayString = "${smartthings.polling-interval-ms:60000}")
	public void poll() {
		for (SmartThingsDevice device : deviceRepository.findByActiveTrueOrderByCreatedAtAsc()) {
			try {
				SensorIngestionResult result = syncService.sync(device.getId(), "POLLING");
				log.debug("SmartThings sensor sync completed device={} savedReadings={}",
						maskedDeviceId(result.deviceId()), result.readings().size());
			} catch (RuntimeException exception) {
				log.warn("SmartThings sensor sync failed device={} reason={}",
						maskedDeviceId(device.getSmartThingsDeviceId()), exception.getMessage());
			}
		}
	}

	private String maskedDeviceId(String deviceId) {
		if (deviceId == null || deviceId.length() <= 8) {
			return "device";
		}
		return deviceId.substring(0, 8) + "...";
	}
}
