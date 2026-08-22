package com.plantdoctor.controller;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plantdoctor.config.SeedProperties;
import com.plantdoctor.seed.PlantDiseaseSeedRunner;
import com.plantdoctor.seed.PlantDiseaseSeedService;
import com.plantdoctor.seed.SeedResult;

/**
 * Manual trigger for CSV seeding (dev/MVP). No auth yet — do not expose publicly in production.
 */
@RestController
@RequestMapping("/api/admin")
public class SeedController {

	private final SeedProperties seedProperties;
	private final PlantDiseaseSeedService seedService;

	public SeedController(SeedProperties seedProperties, PlantDiseaseSeedService seedService) {
		this.seedProperties = seedProperties;
		this.seedService = seedService;
	}

	@PostMapping("/seed")
	public ResponseEntity<Map<String, Object>> seed(
			@RequestParam(name = "csvPath", required = false) String csvPathOverride) {

		String csvPathValue = StringUtils.hasText(csvPathOverride) ? csvPathOverride : seedProperties.getCsvPath();
		SeedResult result = seedService.seedFromCsv(Path.of(csvPathValue));
		PlantDiseaseSeedRunner.logSeedResult(result);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("csvPath", result.csvPath());
		body.put("rowsRead", result.rowsRead());
		body.put("plantsCreated", result.plantsCreated());
		body.put("diseasesInserted", result.diseasesInserted());
		body.put("diseasesSkipped", result.diseasesSkipped());
		body.put("errors", result.errors());

		if (!result.errors().isEmpty() && result.diseasesInserted() == 0 && result.rowsRead() == 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
		}
		return ResponseEntity.ok(body);
	}
}
