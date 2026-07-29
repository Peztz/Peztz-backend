package com.peztz.backend.log.entity;

import java.time.OffsetDateTime;

import com.peztz.backend.admission.entity.AdmissionSession;

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
@Table(name = "pet_videos", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVideo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "video_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private AdmissionSession session;

	@Column(name = "video_path", columnDefinition = "text")
	private String videoPath;

	@Column(name = "thumbnail_path", columnDefinition = "text")
	private String thumbnailPath;

	@Column(name = "start_time")
	private OffsetDateTime startTime;

	@Column(name = "end_time")
	private OffsetDateTime endTime;

	private Integer duration;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now().withNano(0);
		}
	}
}
