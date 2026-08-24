package com.peztz.backend.report.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.peztz.backend.pet.entity.Pet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "daily_report",
		schema = "public",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_daily_report_pet_date",
				columnNames = {"pet_id", "report_date"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReport {

	@Id
	@Column(name = "report_id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pet_id", nullable = false)
	private Pet pet;

	@Column(name = "report_date", nullable = false)
	private LocalDate reportDate;

	@Column(name = "generation_token")
	private UUID generationToken;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "total_log_count", nullable = false)
	private long totalLogCount;

	@Column(name = "sensor_log_count", nullable = false)
	private long sensorLogCount;

	@Column(name = "average_temperature")
	private Double averageTemperature;

	@Column(name = "average_humidity")
	private Double averageHumidity;

	@Column(name = "door_open_count", nullable = false)
	private long doorOpenCount;

	@Column(name = "low_light_count", nullable = false)
	private long lowLightCount;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> content;

	@Column(name = "model_name", length = 100)
	private String modelName;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "generated_at")
	private OffsetDateTime generatedAt;

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
		if (content == null) {
			content = new HashMap<>();
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
