package com.plantdoctor.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple readiness check for Phase 1: confirms the app can reach MySQL and
 * that Flyway created the expected tables.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		try {
			jdbcTemplate.queryForObject("SELECT 1", Integer.class);

			Map<String, Object> body = new LinkedHashMap<>();
			body.put("status", "ok");
			body.put("database", "connected");
			body.put("tables", tableRowCounts());

			return ResponseEntity.ok(body);
		} catch (Exception ex) {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("status", "error");
			body.put("database", "disconnected");
			body.put("message", ex.getMessage());
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
		}
	}

	private Map<String, Long> tableRowCounts() {
		Map<String, Long> counts = new LinkedHashMap<>();
		counts.put("plants", countRows("plants"));
		counts.put("diseases", countRows("diseases"));
		counts.put("queries", countRows("queries"));
		return counts;
	}

	private long countRows(String tableName) {
		Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
		return count != null ? count : 0L;
	}
}
