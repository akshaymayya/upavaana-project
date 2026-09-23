package com.plantdoctor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

import java.util.Locale;

@ConfigurationProperties(prefix = "app.diagnosis")
public class DiagnosisProperties {

	private static final Logger log = LoggerFactory.getLogger(DiagnosisProperties.class);

	public static final String PROVIDER_DEEPSEEK = "deepseek";
	public static final String PROVIDER_OPENAI = "openai";
	public static final String PROVIDER_GROQ = "groq";
	public static final String ENV_ACTIVE_SYNTHESIS_PROVIDER = "ACTIVE_SYNTHESIS_PROVIDER";

	/**
	 * Bound from {@code app.diagnosis.active-synthesis-provider}. YAML interpolates
	 * {@code ${ACTIVE_SYNTHESIS_PROVIDER:deepseek}}, but a profile file can pin a literal
	 * {@code deepseek} and ignore the OS env var. {@link #resolvedProvider()} therefore
	 * prefers {@code ACTIVE_SYNTHESIS_PROVIDER} on the Spring {@link Environment}.
	 */
	private String activeSynthesisProvider = PROVIDER_DEEPSEEK;

	private Environment environment;

	public void attachEnvironment(Environment environment) {
		this.environment = environment;
	}

	public String getActiveSynthesisProvider() {
		return activeSynthesisProvider;
	}

	public void setActiveSynthesisProvider(String activeSynthesisProvider) {
		this.activeSynthesisProvider = activeSynthesisProvider;
	}

	public String resolvedProvider() {
		String fromEnvProperty = environmentValue(ENV_ACTIVE_SYNTHESIS_PROVIDER);
		// Process env/sysprop only when Spring injected Environment (not hand-built unit objects).
		String fromProcess = environment == null ? null
				: firstNonBlank(System.getenv(ENV_ACTIVE_SYNTHESIS_PROVIDER),
						System.getProperty(ENV_ACTIVE_SYNTHESIS_PROVIDER));
		String fromBoundProperty = environmentValue("app.diagnosis.active-synthesis-provider");
		String chosen = firstNonBlank(fromEnvProperty, fromProcess, fromBoundProperty, activeSynthesisProvider);
		return normalizeProvider(chosen);
	}

	@Nullable
	private String environmentValue(String key) {
		if (environment == null) {
			return null;
		}
		return environment.getProperty(key);
	}

	@Nullable
	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	static String normalizeProvider(String raw) {
		if (raw == null || raw.isBlank()) {
			return PROVIDER_DEEPSEEK;
		}
		String normalized = stripWrappingQuotes(raw.trim()).toLowerCase(Locale.ROOT);
		if (PROVIDER_OPENAI.equals(normalized)) {
			return PROVIDER_OPENAI;
		}
		if (PROVIDER_GROQ.equals(normalized)) {
			return PROVIDER_GROQ;
		}
		if (!PROVIDER_DEEPSEEK.equals(normalized)) {
			log.error("Unknown ACTIVE_SYNTHESIS_PROVIDER '{}' — using deepseek", raw);
			return PROVIDER_DEEPSEEK;
		}
		return PROVIDER_DEEPSEEK;
	}

	private static String stripWrappingQuotes(String value) {
		if (value.length() >= 2) {
			char first = value.charAt(0);
			char last = value.charAt(value.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
				return value.substring(1, value.length() - 1).trim();
			}
		}
		return value;
	}
}
