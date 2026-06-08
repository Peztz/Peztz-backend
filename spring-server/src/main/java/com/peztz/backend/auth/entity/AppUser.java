package com.peztz.backend.auth.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

	@Id
	@Column(name = "user_id")
	private UUID id;

	@Column(name = "hospital_id")
	private UUID hospitalId;

	@Column(nullable = false, unique = true, length = 50)
	private String email;

	@Column(name = "password", nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, length = 50)
	private String name;

	@Transient
	private String phoneNumber;

	@Column(nullable = false, length = 50)
	private String role;

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
	}
}
