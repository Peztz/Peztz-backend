package com.peztz.backend.smartthings.device;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peztz.backend.cage.entity.Cage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "smartthings_device", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartThingsDevice {

	@Id
	@Column(name = "smartthings_device_mapping_id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cage_id", nullable = false)
	private Cage cage;

	@Column(name = "smartthings_device_id", nullable = false, unique = true, length = 100)
	private String smartThingsDeviceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "device_type", nullable = false, length = 50)
	private SmartThingsDeviceType deviceType;

	@Column(length = 100)
	private String label;

	@Column
	private Integer battery;

	@Column(nullable = false)
	private boolean online;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "last_seen_at")
	private OffsetDateTime lastSeenAt;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now().withNano(0);
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now().withNano(0);
	}
}
