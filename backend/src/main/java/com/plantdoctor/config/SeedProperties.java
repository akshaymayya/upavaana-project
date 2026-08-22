package com.plantdoctor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

	/**
	 * When true, runs {@link com.plantdoctor.seed.PlantDiseaseSeedService} once after startup.
	 */
	private boolean enabled = false;

	/**
	 * Filesystem path to the CSV (relative paths resolve from the JVM working directory).
	 */
	private String csvPath = "./plant-disease-seed-template.csv";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getCsvPath() {
		return csvPath;
	}

	public void setCsvPath(String csvPath) {
		this.csvPath = csvPath;
	}

}
