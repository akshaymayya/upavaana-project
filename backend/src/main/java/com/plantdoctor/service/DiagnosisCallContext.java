package com.plantdoctor.service;

/**
 * Request-scoped latency budget for the live diagnosis HTTP path.
 */
public final class DiagnosisCallContext {

	private static final ThreadLocal<DiagnosisLatencyBudget> CURRENT = new ThreadLocal<>();

	private DiagnosisCallContext() {
	}

	public static void begin(DiagnosisLatencyBudget budget) {
		CURRENT.set(budget);
	}

	public static void end() {
		CURRENT.remove();
	}

	public static DiagnosisLatencyBudget current() {
		DiagnosisLatencyBudget budget = CURRENT.get();
		return budget != null ? budget : DiagnosisLatencyBudget.defaults();
	}

	public static boolean isActive() {
		return CURRENT.get() != null;
	}
}
