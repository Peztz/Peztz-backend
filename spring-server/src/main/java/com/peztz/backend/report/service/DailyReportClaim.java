package com.peztz.backend.report.service;

import java.util.UUID;

public record DailyReportClaim(UUID reportId, UUID generationToken) {
}
