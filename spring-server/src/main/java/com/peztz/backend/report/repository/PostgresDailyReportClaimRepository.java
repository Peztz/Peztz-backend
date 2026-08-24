package com.peztz.backend.report.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.peztz.backend.report.service.DailyReportClaim;
import com.peztz.backend.report.service.DailyReportStatistics;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostgresDailyReportClaimRepository {

	private static final String CLAIM_SQL = """
			insert into public.daily_report (
			    report_id,
			    pet_id,
			    report_date,
			    generation_token,
			    status,
			    total_log_count,
			    sensor_log_count,
			    average_temperature,
			    average_humidity,
			    door_open_count,
			    low_light_count,
			    content,
			    model_name,
			    error_message,
			    generated_at,
			    created_at,
			    updated_at
			)
			values (
			    :newReportId,
			    :petId,
			    :reportDate,
			    :generationToken,
			    'GENERATING',
			    :totalLogCount,
			    :sensorLogCount,
			    :averageTemperature,
			    :averageHumidity,
			    :doorOpenCount,
			    :lowLightCount,
			    cast('{}' as jsonb),
			    null,
			    null,
			    null,
			    :now,
			    :now
			)
			on conflict (pet_id, report_date) do update set
			    generation_token = excluded.generation_token,
			    status = 'GENERATING',
			    total_log_count = excluded.total_log_count,
			    sensor_log_count = excluded.sensor_log_count,
			    average_temperature = excluded.average_temperature,
			    average_humidity = excluded.average_humidity,
			    door_open_count = excluded.door_open_count,
			    low_light_count = excluded.low_light_count,
			    content = cast('{}' as jsonb),
			    model_name = null,
			    error_message = null,
			    generated_at = null,
			    updated_at = excluded.updated_at
			where (
			       daily_report.status = 'READY'
			       and (
			           daily_report.total_log_count <> excluded.total_log_count
			           or daily_report.sensor_log_count <> excluded.sensor_log_count
			           or daily_report.average_temperature is distinct from excluded.average_temperature
			           or daily_report.average_humidity is distinct from excluded.average_humidity
			           or daily_report.door_open_count <> excluded.door_open_count
			           or daily_report.low_light_count <> excluded.low_light_count
			       )
			    )
			    or (
			       daily_report.status = 'FAILED'
			       and (
			           daily_report.updated_at < :retryBefore
			           or daily_report.total_log_count <> excluded.total_log_count
			           or daily_report.sensor_log_count <> excluded.sensor_log_count
			           or daily_report.average_temperature is distinct from excluded.average_temperature
			           or daily_report.average_humidity is distinct from excluded.average_humidity
			           or daily_report.door_open_count <> excluded.door_open_count
			           or daily_report.low_light_count <> excluded.low_light_count
			       )
			    )
			    or (
			       daily_report.status = 'GENERATING'
			       and daily_report.updated_at < :staleBefore
			    )
			returning report_id, generation_token
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public Optional<DailyReportClaim> tryClaim(
			UUID petId,
			LocalDate reportDate,
			DailyReportStatistics statistics,
			OffsetDateTime now,
			OffsetDateTime retryBefore,
			OffsetDateTime staleBefore) {
		UUID generationToken = UUID.randomUUID();
		MapSqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("newReportId", UUID.randomUUID())
				.addValue("petId", petId)
				.addValue("reportDate", reportDate)
				.addValue("generationToken", generationToken)
				.addValue("totalLogCount", statistics.totalLogCount())
				.addValue("sensorLogCount", statistics.sensorLogCount())
				.addValue("averageTemperature", statistics.averageTemperature())
				.addValue("averageHumidity", statistics.averageHumidity())
				.addValue("doorOpenCount", statistics.doorOpenCount())
				.addValue("lowLightCount", statistics.lowLightCount())
				.addValue("now", now)
				.addValue("retryBefore", retryBefore)
				.addValue("staleBefore", staleBefore);

		List<DailyReportClaim> claims = jdbcTemplate.query(
				CLAIM_SQL,
				parameters,
				(row, rowNumber) -> new DailyReportClaim(
						row.getObject("report_id", UUID.class),
						row.getObject("generation_token", UUID.class)));
		return claims.stream().findFirst();
	}
}
