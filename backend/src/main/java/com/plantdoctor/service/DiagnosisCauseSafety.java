package com.plantdoctor.service;

/**
 * @deprecated Logic consolidated in {@link DiagnosisPipeline}. Retained for unit tests.
 */
@Deprecated
public final class DiagnosisCauseSafety {

	private DiagnosisCauseSafety() {
	}

	public static DiagnosisResult enforce(DiagnosisResult parsed) {
		return DiagnosisPipeline.enforceTreatment(parsed, "", java.util.List.of());
	}
}
