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
}
