package com.plantdoctor.config;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads {@code .env} into system properties and a high-priority property source
 * so rotated keys (e.g. GEMINI_API_KEY) bind even when a stale OS env var exists.
 */
public class EnvFileEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Path cwd = Path.of(System.getProperty("user.dir", "."));
		Path envFile = firstExisting(cwd.resolve(".env"), cwd.resolve("backend").resolve(".env"));
		if (envFile == null) {
			return;
		}
		EnvFileLoader.load(envFile);
		Map<String, Object> dotenv = new LinkedHashMap<>();
		for (String name : new String[] {
				"GEMINI_API_KEY", "GROQ_API_KEY", "NVIDIA_API_KEY",
				"ACTIVE_VISION_PROVIDER", "ACTIVE_SYNTHESIS_PROVIDER",
				"GEMINI_VISION_MODEL", "GROQ_MODEL" }) {
			String value = System.getProperty(name);
			if (value != null && !value.isBlank()) {
				dotenv.put(name, value);
			}
		}
		if (!dotenv.isEmpty()) {
			environment.getPropertySources().addFirst(new MapPropertySource("plantdoctor-dotenv", dotenv));
		}
	}

	private static Path firstExisting(Path... candidates) {
		for (Path candidate : candidates) {
			if (candidate != null && java.nio.file.Files.isRegularFile(candidate)) {
				return candidate;
			}
		}
		return null;
	}
}
