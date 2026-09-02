package com.plantdoctor.service;

/**
 * End-to-end diagnosis deadline plus per-provider HTTP bounds.
 * Gemini leftover time must not shrink the NVIDIA fallback timeout.
 */
public final class DiagnosisLatencyBudget {

	/** Gemini ~8s + NVIDIA ~8s + Groq ~8s + slack. Not a hard 16s phone target. */
	public static final int DEFAULT_TOTAL_MS = 28_000;
	/** Informational Gemini+NVIDIA envelope; not used to cap NVIDIA HTTP. */
	public static final int DEFAULT_VISION_TOTAL_MS = 16_000;
	public static final int DEFAULT_VISION_FALLBACK_MAX_MS = 8_000;
	public static final int DEFAULT_SYNTHESIS_MAX_MS = 8_000;
	public static final int MIN_USEFUL_CALL_MS = 800;

	private final long startEpochMs;
	private final int totalMs;
	private final int visionTotalMs;
	private final int visionFallbackMaxMs;
	private final int synthesisMaxMs;

	public DiagnosisLatencyBudget(
			int totalMs,
			int visionTotalMs,
			int visionFallbackMaxMs,
			int synthesisMaxMs) {
		this.startEpochMs = System.currentTimeMillis();
		this.totalMs = Math.max(MIN_USEFUL_CALL_MS, totalMs);
		this.visionTotalMs = Math.max(MIN_USEFUL_CALL_MS, visionTotalMs);
		this.visionFallbackMaxMs = Math.max(MIN_USEFUL_CALL_MS, visionFallbackMaxMs);
		this.synthesisMaxMs = Math.max(MIN_USEFUL_CALL_MS, synthesisMaxMs);
	}

	public static DiagnosisLatencyBudget defaults() {
		return new DiagnosisLatencyBudget(
				DEFAULT_TOTAL_MS,
				DEFAULT_VISION_TOTAL_MS,
				DEFAULT_VISION_FALLBACK_MAX_MS,
				DEFAULT_SYNTHESIS_MAX_MS);
	}

	public int remainingTotalMs() {
		return remainingUntil(startEpochMs + totalMs);
	}

	public int remainingVisionMs() {
		return remainingUntil(startEpochMs + visionTotalMs);
	}

	public boolean hasUsefulTime(int remainingMs) {
		return remainingMs >= MIN_USEFUL_CALL_MS;
	}

	public boolean canStartProviderCall() {
		return hasUsefulTime(remainingTotalMs());
	}

	public int capTimeout(int configuredMs, int remainingMs) {
		int configured = Math.max(MIN_USEFUL_CALL_MS, configuredMs);
		int remaining = Math.max(0, remainingMs);
		return Math.max(0, Math.min(configured, remaining));
	}

	/**
	 * NVIDIA HTTP read timeout is the configured provider bound (not leftover Gemini time).
	 */
	public int nvidiaFallbackReadTimeoutMs(int configuredReadMs) {
		return Math.max(MIN_USEFUL_CALL_MS, configuredReadMs);
	}

	public int nvidiaFallbackConnectTimeoutMs(int configuredConnectMs) {
		return Math.max(MIN_USEFUL_CALL_MS, configuredConnectMs);
	}

	public int groqReadTimeoutMs(int configuredReadMs) {
		return capTimeout(Math.min(configuredReadMs, synthesisMaxMs), remainingTotalMs());
	}

	public int groqConnectTimeoutMs(int configuredConnectMs) {
		return capTimeout(configuredConnectMs, remainingTotalMs());
	}

	public int visionFallbackMaxMs() {
		return visionFallbackMaxMs;
	}

	public int totalMs() {
		return totalMs;
	}

	public int visionTotalMs() {
		return visionTotalMs;
	}

	public int synthesisMaxMs() {
		return synthesisMaxMs;
	}

	public int elapsedMs() {
		return (int) Math.max(0, System.currentTimeMillis() - startEpochMs);
	}

	private static int remainingUntil(long deadlineEpochMs) {
		return (int) Math.max(0, deadlineEpochMs - System.currentTimeMillis());
	}
}
