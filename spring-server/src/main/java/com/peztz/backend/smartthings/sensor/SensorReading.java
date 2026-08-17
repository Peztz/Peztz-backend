package com.peztz.backend.smartthings.sensor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.peztz.backend.admission.entity.AdmissionSession;
import com.peztz.backend.cage.entity.Cage;
import com.peztz.backend.smartthings.device.SmartThingsDevice;

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
@Table(name = "sensor_reading", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reading_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "smartthings_device_mapping_id", nullable = false)
	private SmartThingsDevice device;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cage_id", nullable = false)
	private Cage cage;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id")
	private AdmissionSession session;

	@Column(nullable = false, length = 100)
	private String capability;

	@Column(nullable = false, length = 100)
	private String attribute;

	@Column(name = "numeric_value", precision = 19, scale = 4)
	private BigDecimal numericValue;

	@Column(name = "string_value", length = 255)
	private String stringValue;

	@Column(length = 30)
	private String unit;

	@Column(name = "measured_at", nullable = false)
	private OffsetDateTime measuredAt;

	@Column(name = "received_at", nullable = false)
	private OffsetDateTime receivedAt;

	@Column(nullable = false, length = 30)
	private String source;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> rawPayload;

	@PrePersist
	void prePersist() {
		if (receivedAt == null) {
			receivedAt = OffsetDateTime.now().withNano(0);
		}
		if (rawPayload == null) {
			rawPayload = new HashMap<>();
		}
	}
}
