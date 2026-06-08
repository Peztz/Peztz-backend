package com.peztz.backend.admission.entity;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.peztz.backend.auth.entity.AppUser;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.pet.entity.Pet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "access_session", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "session_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser owner;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pet_id", nullable = false)
	private Pet pet;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cage_id", nullable = false)
	private Cage cage;

	@Column(name = "access_code", nullable = false, length = 50)
	private String accessCode;

	@Column(nullable = false, length = 50)
	private String status;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "ended_at")
	private OffsetDateTime endedAt;

	public LocalDateTime getStartedAt() {
		return createdAt == null ? null : createdAt.toLocalDateTime();
	}

	public LocalDateTime getEndedAtAsLocalDateTime() {
		return endedAt == null ? null : endedAt.toLocalDateTime();
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now().withNano(0);
		}
	}
}
