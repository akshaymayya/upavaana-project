package com.plantdoctor.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeminiPropertiesTest {

	@Test
	void hasConfiguredApiKey_doesNotTreatPlaceholderAsConfigured() {
		GeminiProperties properties = new GeminiProperties();
		assertFalse(properties.hasConfiguredApiKey());
		properties.setApiKey("your_gemini_api_key_here");
		assertFalse(properties.hasConfiguredApiKey());
		properties.setApiKey("AQ.not_logged");
		assertTrue(properties.hasConfiguredApiKey());
	}
}
