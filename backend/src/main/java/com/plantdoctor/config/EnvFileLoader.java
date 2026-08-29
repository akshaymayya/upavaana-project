package com.plantdoctor.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Spring Boot does not load {@code .env}. Operators often set keys only in that file
 * and confirm with {@code echo}/{@code Get-Content}, while the JVM never sees them.
 */
public final class EnvFileLoader {

	private static final Logger log = LoggerFactory.getLogger(EnvFileLoader.class);

	private EnvFileLoader() {
	}

	public static void loadFirstExisting(Path... candidates) {
		for (Path candidate : candidates) {
			if (candidate != null && Files.isRegularFile(candidate)) {
				load(candidate);
				return;
			}
		}
	}

	static void load(Path file) {
		List<String> lines;
		try {
			lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			log.warn("Could not read env file {}: {}", file.toAbsolutePath(), ex.getMessage());
			return;
		}
		int applied = 0;
		for (String raw : lines) {
			if (applyLine(raw)) {
				applied++;
			}
		}
		log.info("Loaded {} keys from {} into system properties (existing OS env vars were not overwritten)",
				applied, file.toAbsolutePath());
	}

	static boolean applyLine(String raw) {
		if (raw == null) {
			return false;
		}
		String line = raw.trim();
		if (line.isEmpty() || line.startsWith("#")) {
			return false;
		}
		if (line.startsWith("export ")) {
			line = line.substring(7).trim();
		}
		int eq = line.indexOf('=');
		if (eq <= 0) {
			return false;
		}
		String key = line.substring(0, eq).trim();
		String value = stripQuotes(line.substring(eq + 1).trim());
		if (!StringUtils.hasText(key)) {
			return false;
		}
		if (StringUtils.hasText(System.getenv(key)) || StringUtils.hasText(System.getProperty(key))) {
			return false;
		}
		System.setProperty(key, value);
		return true;
	}

	private static String stripQuotes(String value) {
		if (value.length() >= 2) {
			char first = value.charAt(0);
			char last = value.charAt(value.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
				return value.substring(1, value.length() - 1);
			}
		}
		return value;
	}
}
