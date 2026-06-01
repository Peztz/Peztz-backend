package com.peztz.device.repository;

import java.util.List;
import java.util.Optional;

import com.peztz.device.entity.Device;

public interface DeviceRepository {

	Device save(Device device);

	Optional<Device> findByMacAddress(String macAddress);

	List<Device> findAll();
}
