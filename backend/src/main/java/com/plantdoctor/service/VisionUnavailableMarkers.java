package com.plantdoctor.service;

/**
 * Distinguishes vision-provider failure from incomplete vision and genuine healthy outcomes.
 */
public final class VisionUnavailableMarkers {

	private VisionUnavailableMarkers() {
	}

	public static boolean isUnavailableDiagnosis(DiagnosisResult result) {
		if (result == null) {
			return false;
		}
		String disease = nullToEmpty(result.disease_name());
		if (DiagnosisPipeline.VISION_UNAVAILABLE.equalsIgnoreCase(disease.trim())
				|| DiagnosisPipeline.SYNTHESIS_UNAVAILABLE.equalsIgnoreCase(disease.trim())) {
			return true;
		}
		String note = nullToEmpty(result.confidence_note()).toLowerCase();
		return note.contains("diagnostic: unavailable")
				|| note.contains("vision service temporarily unavailable")
				|| note.contains("vision analysis was unavailable");
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
