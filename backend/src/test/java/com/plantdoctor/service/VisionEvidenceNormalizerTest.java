package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VisionEvidenceNormalizerTest {

	@Test
	void completeAssessmentUnchanged() {
		String vision = VisionTestFixtures.COMPLETE_HEALTHY_ASSESSMENT;
		assertEquals(vision, VisionEvidenceNormalizer.normalize(vision));
	}

	@Test
	void incompleteAddsUnknownSections() {
		String normalized = VisionEvidenceNormalizer.normalize(VisionTestFixtures.INCOMPLETE_MORPHOLOGY_ONLY);
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(normalized);
		assertTrue(assessment.isAssessmentComplete());
		assertTrue(normalized.contains("TISSUE DAMAGE: UNKNOWN"));
		assertTrue(normalized.contains("PEST SURFACE SCAN: UNKNOWN"));
	}

	@Test
	void blankBecomesAllUnknown() {
		String normalized = VisionEvidenceNormalizer.normalize("  ");
		assertTrue(normalized.contains("UNKNOWN"));
		assertFalse(VisionObservationAssessment.parse(normalized).allCategoriesExplicitlyNegative());
	}
}
