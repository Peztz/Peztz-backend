package com.peztz.backend.smartthings.device;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartThingsDeviceRepository extends JpaRepository<SmartThingsDevice, UUID> {

	Optional<SmartThingsDevice> findBySmartThingsDeviceId(String smartThingsDeviceId);

	List<SmartThingsDevice> findByCageIdAndActiveTrueOrderByCreatedAtAsc(UUID cageId);

	List<SmartThingsDevice> findByActiveTrueOrderByCreatedAtAsc();
}
