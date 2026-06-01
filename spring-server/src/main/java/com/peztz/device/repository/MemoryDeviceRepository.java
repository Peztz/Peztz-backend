package com.peztz.device.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.peztz.device.entity.Device;

@Repository
public class MemoryDeviceRepository implements DeviceRepository {

	private final Map<String, Device> devices = new ConcurrentHashMap<>();
	private final AtomicLong sequence = new AtomicLong(0);

	@Override
	public Device save(Device device) {
		return devices.compute(device.getMacAddress(), (macAddress, existingDevice) -> {
			if (device.getId() == null) {
				Long id = existingDevice == null
						? sequence.incrementAndGet()
						: existingDevice.getId();
				device.setId(id);
			}
			return device;
		});
	}

	@Override
	public Optional<Device> findByMacAddress(String macAddress) {
		return Optional.ofNullable(devices.get(macAddress));
	}

	@Override
	public List<Device> findAll() {
		return devices.values().stream()
				.sorted(Comparator.comparing(Device::getId))
				.toList();
	}
}
