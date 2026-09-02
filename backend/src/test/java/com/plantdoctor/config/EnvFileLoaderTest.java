package com.plantdoctor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvFileLoaderTest {

	private static final String TEST_KEY = "UPAVANA_DOTENV_LOADER_TEST";

	@AfterEach
	void clearTestProperty() {
		System.clearProperty(TEST_KEY);
	}

	@Test
	void loadAppliesQuotedValueWhenProcessEnvMissing(@TempDir Path dir) throws Exception {
		Path envFile = dir.resolve(".env");
		Files.writeString(envFile, TEST_KEY + "=\"groq\"\n");
		EnvFileLoader.load(envFile);
		assertEquals("groq", System.getProperty(TEST_KEY));
	}

	@Test
	void applyLineSkipsCommentsAndEmpty() {
		assertFalse(EnvFileLoader.applyLine("# " + TEST_KEY + "=groq"));
		assertFalse(EnvFileLoader.applyLine(""));
		assertTrue(EnvFileLoader.applyLine(TEST_KEY + "=groq"));
		assertEquals("groq", System.getProperty(TEST_KEY));
	}

	@Test
	void applyLineOverwritesStaleSystemProperty() {
		System.setProperty(TEST_KEY, "old-stale-value");
		assertTrue(EnvFileLoader.applyLine(TEST_KEY + "=rotated-value"));
		assertEquals("rotated-value", System.getProperty(TEST_KEY));
	}

	@Test
	void loadAppliesGeminiApiKeyOverStaleSystemProperty(@TempDir Path dir) throws Exception {
		String previous = System.getProperty("GEMINI_API_KEY");
		String previousVision = System.getProperty("ACTIVE_VISION_PROVIDER");
		try {
			System.setProperty("GEMINI_API_KEY", "stale-gemini-key");
			Path envFile = dir.resolve(".env");
			Files.writeString(envFile, "GEMINI_API_KEY=rotated-gemini-key\nACTIVE_VISION_PROVIDER=gemini\n");
			EnvFileLoader.load(envFile);
			assertEquals("rotated-gemini-key", System.getProperty("GEMINI_API_KEY"));
			assertEquals("gemini", System.getProperty("ACTIVE_VISION_PROVIDER"));
		} finally {
			if (previous == null) {
				System.clearProperty("GEMINI_API_KEY");
			} else {
				System.setProperty("GEMINI_API_KEY", previous);
			}
			if (previousVision == null) {
				System.clearProperty("ACTIVE_VISION_PROVIDER");
			} else {
				System.setProperty("ACTIVE_VISION_PROVIDER", previousVision);
			}
		}
	}

	@Test
	void loadAppliesGroqApiKeyWhenMissing(@TempDir Path dir) throws Exception {
		String previous = System.getProperty("GROQ_API_KEY");
		try {
			System.clearProperty("GROQ_API_KEY");
			Path envFile = dir.resolve(".env");
			Files.writeString(envFile, "GROQ_API_KEY=gsk_loaded_from_dotenv\nACTIVE_SYNTHESIS_PROVIDER=groq\n");
			EnvFileLoader.load(envFile);
			assertEquals("gsk_loaded_from_dotenv", System.getProperty("GROQ_API_KEY"));
		} finally {
			if (previous == null) {
				System.clearProperty("GROQ_API_KEY");
			} else {
				System.setProperty("GROQ_API_KEY", previous);
			}
		}
	}
}
