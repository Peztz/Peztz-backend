package com.peztz.backend.event.entity;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.peztz.backend.camera.entity.Camera;
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
@Table(name = "pet_event", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetEvent {

	@Id
	@Column(name = "event_id")
	private UUID id;

	@Column(name = "external_event_id", nullable = false, unique = true, length = 100)
	private String externalEventId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pet_id", nullable = false)
	private Pet pet;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "camera_id", nullable = false)
	private Camera camera;

	@Column(name = "event_type", nullable = false, length = 50)
	private String eventType;

	@Column(nullable = false)
	private Double confidence;

	@Column(name = "occurred_at", nullable = false)
	private OffsetDateTime occurredAt;

	@Column(name = "video_url", columnDefinition = "text")
	private String videoUrl;

	@Column(name = "thumbnail_url", columnDefinition = "text")
	private String thumbnailUrl;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> metadata;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (metadata == null) {
			metadata = new HashMap<>();
		}
		if (createdAt == null) {
			createdAt = OffsetDateTime.now().withNano(0);
		}
	}
}
