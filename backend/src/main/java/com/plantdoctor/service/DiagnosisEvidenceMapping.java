package com.plantdoctor.service;

import java.util.List;
import java.util.Locale;

import com.plantdoctor.service.DiseaseCandidate.MatchType;
import com.plantdoctor.service.VisualDiagnosisSupport.Category;

/**
 * @deprecated Use {@link DiagnosisPipeline#enforce}. Retained for unit tests and confidence-note helper.
 */
@Deprecated
public final class DiagnosisEvidenceMapping {

	private DiagnosisEvidenceMapping() {
	}

	public static DiagnosisResult enforce(
			DiagnosisResult parsed,
			String visionText,
			List<DiseaseCandidate> candidates) {
		return DiagnosisPipeline.mapDiagnosis(parsed, visionText, candidates);
	}

	static String diagnosticConfidenceNote(Category visual, List<DiseaseCandidate> candidates) {
		String diagnosticLevel;
		String diagnosticReason;
		switch (visual) {
			case SCALE_INFESTATION, APHID_INFESTATION, WHITEFLY_INFESTATION, MEALYBUG_INFESTATION,
					MITE_INFESTATION, CHEWING_PEST_DAMAGE, PEST_INFESTATION -> {
				diagnosticLevel = "Medium";
				diagnosticReason = "visual evidence supports a pest infestation (exact species not confirmed)";
			}
			case PHYSICAL_LEAF_DAMAGE -> {
				diagnosticLevel = "Medium";
				diagnosticReason = "visible physical tissue damage (holes/missing tissue); pest cause not confirmed";
			}
			case LEAF_SPOT_DISEASE, PLANT_DISEASE -> {
				diagnosticLevel = "Medium";
				diagnosticReason = "visual evidence supports a disease pattern (exact pathogen not confirmed)";
			}
			case UNCERTAIN_PEST -> {
				diagnosticLevel = "Low";
				diagnosticReason = "possible pest structures visible; identification uncertain";
			}
			default -> {
				diagnosticLevel = "Low";
				diagnosticReason = "insufficient visual evidence for a specific diagnosis";
			}
		}

		boolean plantKb = candidates != null && candidates.stream().anyMatch(c -> c.matchType() == MatchType.PLANT_NAME);
		boolean symptomKb = candidates != null && !plantKb
				&& candidates.stream().anyMatch(c -> c.matchType() == MatchType.SYMPTOM_PATTERN);

		String retrieval = plantKb
				? "Retrieval: High (plant in KB)."
				: symptomKb
						? "Retrieval: Medium (symptom pattern only — does not override strong visual evidence)."
						: "Retrieval: Low (no KB candidate).";

		return "Diagnostic: " + diagnosticLevel + " — " + diagnosticReason + ". " + retrieval;
	}
}
