package com.plantdoctor.service;

/**
 * @deprecated Logic consolidated in {@link DiagnosisPipeline}. Retained for unit tests.
 */
@Deprecated
public final class DiagnosisVisionAlignment {

	private DiagnosisVisionAlignment() {
	}

	public static DiagnosisResult enforce(DiagnosisResult parsed, String visionText) {
		return DiagnosisPipeline.alignVisualEvidence(parsed, visionText);
	}
}
