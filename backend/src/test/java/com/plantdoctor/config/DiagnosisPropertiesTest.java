package com.plantdoctor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DiagnosisPropertiesTest {

	@Test
	void envVarGroqWinsOverBoundDeepSeekDefault() {
		DiagnosisProperties properties = new DiagnosisProperties();
		properties.setActiveSynthesisProvider("deepseek");
		MockEnvironment env = new MockEnvironment();
		env.setProperty("ACTIVE_SYNTHESIS_PROVIDER", "groq");
		env.setProperty("app.diagnosis.active-synthesis-provider", "deepseek");
		properties.attachEnvironment(env);

		assertEquals(DiagnosisProperties.PROVIDER_GROQ, properties.resolvedProvider());
	}

	@Test
	void quotedEnvVarGroqIsAccepted() {
		DiagnosisProperties properties = new DiagnosisProperties();
		properties.setActiveSynthesisProvider("deepseek");
		MockEnvironment env = new MockEnvironment();
		env.setProperty("ACTIVE_SYNTHESIS_PROVIDER", "\"groq\"");
		properties.attachEnvironment(env);

		assertEquals(DiagnosisProperties.PROVIDER_GROQ, properties.resolvedProvider());
	}

	@Test
	void systemPropertyGroqIsUsedWhenSpringPropertyMissing() {
		String previous = System.getProperty(DiagnosisProperties.ENV_ACTIVE_SYNTHESIS_PROVIDER);
		try {
			System.setProperty(DiagnosisProperties.ENV_ACTIVE_SYNTHESIS_PROVIDER, "groq");
			DiagnosisProperties properties = new DiagnosisProperties();
			properties.setActiveSynthesisProvider("deepseek");
			properties.attachEnvironment(new MockEnvironment());
			assertEquals(DiagnosisProperties.PROVIDER_GROQ, properties.resolvedProvider());
		} finally {
			if (previous == null) {
				System.clearProperty(DiagnosisProperties.ENV_ACTIVE_SYNTHESIS_PROVIDER);
			} else {
				System.setProperty(DiagnosisProperties.ENV_ACTIVE_SYNTHESIS_PROVIDER, previous);
			}
		}
	}

	@Test
	void defaultSynthesisProviderIsGroq() {
		DiagnosisProperties properties = new DiagnosisProperties();
		assertEquals(DiagnosisProperties.PROVIDER_GROQ, properties.resolvedProvider());
	}

	@Test
	void unknownSynthesisProviderFallsBackToGroq() {
		DiagnosisProperties properties = new DiagnosisProperties();
		properties.setActiveSynthesisProvider("foo");
		assertEquals(DiagnosisProperties.PROVIDER_GROQ, properties.resolvedProvider());
	}

	@Test
	void explicitDeepSeekStillResolvesWhenRequested() {
		DiagnosisProperties properties = new DiagnosisProperties();
		properties.setActiveSynthesisProvider("deepseek");
		assertEquals(DiagnosisProperties.PROVIDER_DEEPSEEK, properties.resolvedProvider());
	}

	@Test
	void defaultVisionProviderIsGemini() {
		DiagnosisProperties properties = new DiagnosisProperties();
		assertEquals(DiagnosisProperties.VISION_PROVIDER_GEMINI, properties.resolvedVisionProvider());
	}

	@Test
	void envVarNvidiaSelectsNvidiaVision() {
		DiagnosisProperties properties = new DiagnosisProperties();
		MockEnvironment env = new MockEnvironment();
		env.setProperty("ACTIVE_VISION_PROVIDER", "nvidia");
		properties.attachEnvironment(env);

		assertEquals(DiagnosisProperties.VISION_PROVIDER_NVIDIA, properties.resolvedVisionProvider());
	}

	@Test
	void visionFallbackDefaultsToNvidiaWhenNotDisabled() {
		DiagnosisProperties properties = new DiagnosisProperties();
		assertEquals(DiagnosisProperties.VISION_PROVIDER_NVIDIA, properties.resolvedVisionFallbackProvider());
		assertTrue(properties.hasVisionFallback());
	}

	@Test
	void visionFallbackNoneDisablesFallback() {
		DiagnosisProperties properties = new DiagnosisProperties();
		properties.setVisionFallbackProvider(DiagnosisProperties.VISION_FALLBACK_NONE);
		assertNull(properties.resolvedVisionFallbackProvider());
		assertFalse(properties.hasVisionFallback());
	}

	@Test
	void latencyBudgetDefaultsMatchProduction() {
		DiagnosisProperties properties = new DiagnosisProperties();
		assertEquals(28000, properties.getLatencyTotalMs());
		assertEquals(16000, properties.getLatencyVisionTotalMs());
		assertEquals(8000, properties.getLatencyVisionFallbackMaxMs());
		assertEquals(8000, properties.getLatencySynthesisMaxMs());
	}
}
