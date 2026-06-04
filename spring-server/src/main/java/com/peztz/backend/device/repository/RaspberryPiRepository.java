package com.peztz.backend.device.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.device.entity.RaspberryPi;

public interface RaspberryPiRepository extends JpaRepository<RaspberryPi, UUID> {

	Optional<RaspberryPi> findByMacAddress(String macAddress);
}
