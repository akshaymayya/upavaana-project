package com.plantdoctor.seed;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.plantdoctor.config.SeedProperties;

/**
 * Optionally seeds the database once when the server starts ({@code app.seed.enabled=true}).
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class PlantDiseaseSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(PlantDiseaseSeedRunner.class);

	private final SeedProperties seedProperties;
	private final PlantDiseaseSeedService seedService;

	public PlantDiseaseSeedRunner(SeedProperties seedProperties, PlantDiseaseSeedService seedService) {
		this.seedProperties = seedProperties;
		this.seedService = seedService;
	}

	@Override
	public void run(ApplicationArguments args) {
		Path csvPath = Path.of(seedProperties.getCsvPath());
		log.info("Startup seed enabled; loading CSV from {}", csvPath.toAbsolutePath().normalize());
		SeedResult result = seedService.seedFromCsv(csvPath);
		logSeedResult(result);
	}

	static void logSeedResult(SeedResult result) {
		log.info(
				"Seed finished for {} — rows: {}, plants created: {}, diseases inserted: {}, skipped: {}, errors: {}",
				result.csvPath(),
				result.rowsRead(),
				result.plantsCreated(),
				result.diseasesInserted(),
				result.diseasesSkipped(),
				result.errors().size());
		for (String error : result.errors()) {
			log.warn("Seed row error: {}", error);
		}
	}
}
