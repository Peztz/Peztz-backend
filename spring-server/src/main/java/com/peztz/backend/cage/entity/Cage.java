package com.peztz.backend.cage.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.facility.entity.Facility;
import com.peztz.backend.pet.entity.Pet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cage", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cage {

	@Id
	@Column(name = "cage_id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hospital_id")
	private Facility facility;

	@Column(length = 100)
	private String name;

	@Column(name = "cage_number", length = 50)
	private String cageNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private AppUser user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "current_pet_id")
	private Pet currentPet;

	@Column(name = "access_code", length = 50)
	private String accessCode;

	@Column(nullable = false, length = 50)
	private String status;

	@Column(name = "device_id")
	private UUID raspberryPiDeviceId;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (createdAt == null) {
			createdAt = LocalDateTime.now().withNano(0);
		}
	}
}
