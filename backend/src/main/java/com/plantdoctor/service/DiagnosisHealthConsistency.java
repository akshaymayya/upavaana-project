package com.plantdoctor.service;

import java.util.List;

/**
 * @deprecated Logic consolidated in {@link DiagnosisPipeline}. Retained for unit tests and stable imports.
 */
@Deprecated
public final class DiagnosisHealthConsistency {

	static final String UNIDENTIFIED = DiagnosisPipeline.UNIDENTIFIED;

	private DiagnosisHealthConsistency() {
	}

	public static DiagnosisResult enforce(DiagnosisResult parsed) {
		return enforce(parsed, null);
	}

	public static DiagnosisResult enforce(DiagnosisResult parsed, String visionText) {
		if (parsed == null) {
			return parsed;
		}
		return DiagnosisPipeline.enforce(parsed, visionText, List.of());
	}

	static boolean contradictsHealthy(DiagnosisResult parsed, String visionText) {
		return DiagnosisPipeline.finalizeHealth(parsed, visionText) != parsed
				|| DiagnosisPipeline.alignVisualEvidence(parsed, visionText) != parsed;
	}

	static boolean claimsHealthyOutcome(DiagnosisResult parsed) {
		return DiagnosisPipeline.claimsHealthyOutcome(parsed);
	}

	static boolean symptomsDescribeIssue(String symptoms) {
		return DiagnosisPipeline.symptomsDescribeIssue(symptoms);
	}
}
