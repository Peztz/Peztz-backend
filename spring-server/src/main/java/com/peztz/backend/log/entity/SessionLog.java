package com.peztz.backend.log.entity;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import com.peztz.backend.admission.entity.AdmissionSession;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "pet_logs", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "log_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private AdmissionSession session;

	@Column(name = "video_id")
	private Long videoId;

	@Column(name = "log_type", nullable = false, length = 50)
	private String type;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "data", nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> data;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	public String getMessage() {
		Object value = getDataValue("message");
		return value == null ? null : value.toString();
	}

	public Double getTemperature() {
		return getDoubleValue("temperature");
	}

	public Double getHumidity() {
		return getDoubleValue("humidity");
	}

	public LocalDateTime getCreatedAtAsLocalDateTime() {
		return createdAt == null ? null : createdAt.toLocalDateTime();
	}

	@PrePersist
	void prePersist() {
		if (data == null) {
			data = new HashMap<>();
		}
		if (createdAt == null) {
			createdAt = OffsetDateTime.now().withNano(0);
		}
	}

	private Object getDataValue(String key) {
		return data == null ? null : data.get(key);
	}

	private Double getDoubleValue(String key) {
		Object value = getDataValue(key);
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		if (value instanceof String text) {
			try {
				return Double.valueOf(text);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
