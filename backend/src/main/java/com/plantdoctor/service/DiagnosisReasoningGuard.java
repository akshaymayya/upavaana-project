package com.plantdoctor.service;

import java.util.List;

/**
 * @deprecated Logic consolidated in {@link DiagnosisPipeline}. Retained for unit tests.
 */
@Deprecated
public final class DiagnosisReasoningGuard {

	private DiagnosisReasoningGuard() {
	}

	public static DiagnosisResult enforce(
			DiagnosisResult parsed,
			String visionText,
			List<DiseaseCandidate> candidates) {
		return DiagnosisPipeline.enforceTreatment(parsed, visionText, candidates);
	}

	static boolean hasStrongPestEvidence(String text) {
		return VisualEvidencePatterns.hasPestEvidence(text);
	}

	static boolean hasStrongPestEvidence(String visionText, String symptomsText) {
		return VisualEvidencePatterns.hasCrediblePestEvidence(visionText, symptomsText);
	}

	static boolean looksLikeLesionNotPest(String visionAndSymptoms) {
		return DiagnosisPipeline.looksLikeLesionNotPest(visionAndSymptoms);
	}

	static String stripNegatedPestClauses(String text) {
		return VisualEvidencePatterns.stripNegatedClauses(text);
	}
}
