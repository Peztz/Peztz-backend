package com.peztz.backend.log.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.peztz.backend.log.entity.SessionVideo;

public interface SessionVideoRepository extends JpaRepository<SessionVideo, Long> {
}
