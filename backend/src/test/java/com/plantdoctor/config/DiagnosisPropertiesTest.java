package com.plantdoctor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
