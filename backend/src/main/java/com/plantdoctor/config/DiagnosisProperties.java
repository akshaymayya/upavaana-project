package com.plantdoctor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

import com.plantdoctor.service.DiagnosisLatencyBudget;

import java.util.Locale;

@ConfigurationProperties(prefix = "app.diagnosis")
public class DiagnosisProperties {

	private static final Logger log = LoggerFactory.getLogger(DiagnosisProperties.class);

	public static final String PROVIDER_DEEPSEEK = "deepseek";
	public static final String PROVIDER_OPENAI = "openai";
	public static final String PROVIDER_GROQ = "groq";
	public static final String ENV_ACTIVE_SYNTHESIS_PROVIDER = "ACTIVE_SYNTHESIS_PROVIDER";

	public static final String VISION_PROVIDER_GEMINI = "gemini";
	public static final String VISION_PROVIDER_NVIDIA = "nvidia";
	public static final String VISION_FALLBACK_NONE = "none";
	public static final String ENV_ACTIVE_VISION_PROVIDER = "ACTIVE_VISION_PROVIDER";
	public static final String ENV_VISION_FALLBACK_PROVIDER = "VISION_FALLBACK_PROVIDER";

	/**
	 * Bound from {@code app.diagnosis.active-synthesis-provider}. YAML interpolates
	 * {@code ${ACTIVE_SYNTHESIS_PROVIDER:groq}}, but a profile file can pin a literal
	 * value and ignore the OS env var. {@link #resolvedProvider()} therefore
	 * prefers {@code ACTIVE_SYNTHESIS_PROVIDER} on the Spring {@link Environment}.
	 */
	private String activeSynthesisProvider = PROVIDER_GROQ;

	/**
	 * Bound from {@code app.diagnosis.active-vision-provider}. MVP default: gemini.
	 */
	private String activeVisionProvider = VISION_PROVIDER_GEMINI;

	/**
	 * Optional fallback when the primary vision provider fails transiently (503/429/timeout/network).
	 * Use {@code none} to disable. Default: nvidia when primary is gemini.
	 */
	private String visionFallbackProvider = VISION_PROVIDER_NVIDIA;

	private int latencyTotalMs = DiagnosisLatencyBudget.DEFAULT_TOTAL_MS;
	private int latencyVisionTotalMs = DiagnosisLatencyBudget.DEFAULT_VISION_TOTAL_MS;
	private int latencyVisionFallbackMaxMs = DiagnosisLatencyBudget.DEFAULT_VISION_FALLBACK_MAX_MS;
	private int latencySynthesisMaxMs = DiagnosisLatencyBudget.DEFAULT_SYNTHESIS_MAX_MS;

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

	public String getActiveVisionProvider() {
		return activeVisionProvider;
	}

	public void setActiveVisionProvider(String activeVisionProvider) {
		this.activeVisionProvider = activeVisionProvider;
	}

	public String getVisionFallbackProvider() {
		return visionFallbackProvider;
	}

	public void setVisionFallbackProvider(String visionFallbackProvider) {
		this.visionFallbackProvider = visionFallbackProvider;
	}

	public int getLatencyTotalMs() {
		return latencyTotalMs;
	}

	public void setLatencyTotalMs(int latencyTotalMs) {
		this.latencyTotalMs = latencyTotalMs;
	}

	public int getLatencyVisionTotalMs() {
		return latencyVisionTotalMs;
	}

	public void setLatencyVisionTotalMs(int latencyVisionTotalMs) {
		this.latencyVisionTotalMs = latencyVisionTotalMs;
	}

	public int getLatencyVisionFallbackMaxMs() {
		return latencyVisionFallbackMaxMs;
	}

	public void setLatencyVisionFallbackMaxMs(int latencyVisionFallbackMaxMs) {
		this.latencyVisionFallbackMaxMs = latencyVisionFallbackMaxMs;
	}

	public int getLatencySynthesisMaxMs() {
		return latencySynthesisMaxMs;
	}

	public void setLatencySynthesisMaxMs(int latencySynthesisMaxMs) {
		this.latencySynthesisMaxMs = latencySynthesisMaxMs;
	}

	public DiagnosisLatencyBudget newLatencyBudget() {
		return new DiagnosisLatencyBudget(
				latencyTotalMs, latencyVisionTotalMs, latencyVisionFallbackMaxMs, latencySynthesisMaxMs);
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

	public String resolvedVisionProvider() {
		String fromEnvProperty = environmentValue(ENV_ACTIVE_VISION_PROVIDER);
		String fromProcess = environment == null ? null
				: firstNonBlank(System.getenv(ENV_ACTIVE_VISION_PROVIDER),
						System.getProperty(ENV_ACTIVE_VISION_PROVIDER));
		String fromBoundProperty = environmentValue("app.diagnosis.active-vision-provider");
		String chosen = firstNonBlank(fromEnvProperty, fromProcess, fromBoundProperty, activeVisionProvider);
		return normalizeVisionProvider(chosen);
	}

	public boolean useNvidiaVision() {
		return VISION_PROVIDER_NVIDIA.equals(resolvedVisionProvider());
	}

	public boolean useGeminiVision() {
		return VISION_PROVIDER_GEMINI.equals(resolvedVisionProvider());
	}

	@Nullable
	public String resolvedVisionFallbackProvider() {
		String fromEnvProperty = environmentValue(ENV_VISION_FALLBACK_PROVIDER);
		String fromProcess = environment == null ? null
				: firstNonBlank(System.getenv(ENV_VISION_FALLBACK_PROVIDER),
						System.getProperty(ENV_VISION_FALLBACK_PROVIDER));
		String fromBoundProperty = environmentValue("app.diagnosis.vision-fallback-provider");
		String chosen = firstNonBlank(fromEnvProperty, fromProcess, fromBoundProperty, visionFallbackProvider);
		if (chosen == null || chosen.isBlank() || VISION_FALLBACK_NONE.equalsIgnoreCase(chosen.trim())) {
			return null;
		}
		return normalizeVisionProvider(chosen);
	}

	public boolean hasVisionFallback() {
		String fallback = resolvedVisionFallbackProvider();
		return fallback != null && !fallback.equalsIgnoreCase(resolvedVisionProvider());
	}

	public boolean useOpenAi() {
		return PROVIDER_OPENAI.equals(resolvedProvider());
	}

	public boolean useGroq() {
		return PROVIDER_GROQ.equals(resolvedProvider());
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
			return PROVIDER_GROQ;
		}
		String normalized = stripWrappingQuotes(raw.trim()).toLowerCase(Locale.ROOT);
		if (PROVIDER_OPENAI.equals(normalized)) {
			return PROVIDER_OPENAI;
		}
		if (PROVIDER_DEEPSEEK.equals(normalized)) {
			return PROVIDER_DEEPSEEK;
		}
		if (!PROVIDER_GROQ.equals(normalized)) {
			log.error("Unknown ACTIVE_SYNTHESIS_PROVIDER '{}' — using groq", raw);
			return PROVIDER_GROQ;
		}
		return PROVIDER_GROQ;
	}

	static String normalizeVisionProvider(String raw) {
		if (raw == null || raw.isBlank()) {
			return VISION_PROVIDER_GEMINI;
		}
		String normalized = stripWrappingQuotes(raw.trim()).toLowerCase(Locale.ROOT);
		if (VISION_PROVIDER_NVIDIA.equals(normalized)) {
			return VISION_PROVIDER_NVIDIA;
		}
		if (!VISION_PROVIDER_GEMINI.equals(normalized)) {
			log.error("Unknown ACTIVE_VISION_PROVIDER '{}' — using gemini", raw);
			return VISION_PROVIDER_GEMINI;
		}
		return VISION_PROVIDER_GEMINI;
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
