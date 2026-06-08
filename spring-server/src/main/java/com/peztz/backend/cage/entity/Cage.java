package com.peztz.backend.cage.entity;

import java.util.UUID;

import com.peztz.backend.auth.entity.AppUser;
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

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
	}
}
