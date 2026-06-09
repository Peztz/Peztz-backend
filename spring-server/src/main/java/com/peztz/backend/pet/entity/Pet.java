package com.peztz.backend.pet.entity;

import java.util.UUID;
import java.time.LocalDate;

import com.peztz.backend.auth.entity.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "`Pets`", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pet {

	@Id
	@Column(name = "pet_id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser owner;

	@Column(nullable = false, length = 50)
	private String name;

	@Transient
	private String species;

	@Column(name = "pet_breed", nullable = false, length = 50)
	private String breed;

	@Transient
	private String gender;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Transient
	private Double weightKg;

	@Column(name = "medical_note", columnDefinition = "text")
	private String memo;

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
	}
}
