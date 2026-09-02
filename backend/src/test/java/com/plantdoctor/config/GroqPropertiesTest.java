package com.plantdoctor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GroqPropertiesTest {

	@Test
	void defaultTimeoutsAreBounded() {
		GroqProperties properties = new GroqProperties();
		assertEquals(4000, properties.getConnectTimeoutMs());
		assertEquals(8000, properties.getReadTimeoutMs());
	}

	@Test
	void hasConfiguredApiKey_doesNotTreatPlaceholderAsConfigured() {
		GroqProperties properties = new GroqProperties();
		assertFalse(properties.hasConfiguredApiKey());
		properties.setApiKey("your_groq_api_key_here");
		assertFalse(properties.hasConfiguredApiKey());
		properties.setApiKey("gsk_not_logged");
		assertTrue(properties.hasConfiguredApiKey());
	}
}
