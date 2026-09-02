package com.plantdoctor.service;

/**
 * Ensures Gemini and NVIDIA vision text share the PRESENT/ABSENT/UNKNOWN contract.
 * Missing required sections are labeled UNKNOWN — never implied ABSENT or Healthy.
 */
public final class VisionEvidenceNormalizer {

	private VisionEvidenceNormalizer() {
	}

	public static String normalize(String visionText) {
		if (visionText == null || visionText.isBlank()) {
			return "TISSUE DAMAGE: UNKNOWN\nPEST SURFACE SCAN: UNKNOWN\nDISEASE SIGNS: UNKNOWN\nABIOTIC STRESS: UNKNOWN";
		}
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(visionText);
		if (assessment.isAssessmentComplete()) {
			return visionText;
		}
		if (assessment.canUseFreeTextEvidence()) {
			return visionText;
		}
		StringBuilder out = new StringBuilder(visionText.trim());
		for (String section : assessment.missingRequiredSections()) {
			out.append('\n').append(section).append(": UNKNOWN");
		}
		return out.toString();
	}
}
