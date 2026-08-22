package com.plantdoctor.service;

import com.plantdoctor.entity.Disease;

/**
 * A knowledge-base disease paired with how it was retrieved for RAG synthesis.
 */
public record DiseaseCandidate(Disease disease, MatchType matchType) {

	public enum MatchType {
		/** Vision text matched this record's plant name or common names. */
		PLANT_NAME,
		/** Vision symptoms/disease patterns matched this record across any plant species. */
		SYMPTOM_PATTERN
	}
}
