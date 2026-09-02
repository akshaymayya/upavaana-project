package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VisualEvidencePatternsTest {

	@Test
	void visibleCaterpillar_isPestEvidenceWithoutRequiringStructuredSections() {
		String vision = "Leaf with multiple feeding holes and a caterpillar visible on the underside.";
		assertTrue(VisualEvidencePatterns.hasPestEvidence(vision));
		assertTrue(VisualEvidencePatterns.hasCrediblePestEvidence(vision));
		assertEquals(VisualDiagnosisSupport.Category.CHEWING_PEST_DAMAGE,
				VisualDiagnosisSupport.classifyFromVision(vision));
	}

	@Test
	void holesAndRaggedMargins_arePhysicalDamageEvenIfTissueLineSaysNoneSeen() {
		String vision = "Large missing sections and ragged margins. TISSUE DAMAGE: none seen. "
				+ "PEST SURFACE SCAN: none seen. DISEASE SIGNS: none seen. ABIOTIC STRESS: none seen.";
		assertTrue(VisualEvidencePatterns.hasPhysicalDamageEvidence(vision));
		assertTrue(VisualEvidencePatterns.hasCrediblePhysicalDamageEvidence(vision));
		assertFalse(VisualEvidencePatterns.supportsHealthyConclusion(vision, "none", "Continue care"));
	}

	@Test
	void hasPestEvidence_detectsScaleWithoutTissueDamage() {
		String vision = "Green leaf with numerous scale-like insects attached along the midrib; no holes or yellowing.";
		assertTrue(VisualEvidencePatterns.hasPestEvidence(vision));
		assertFalse(VisualEvidencePatterns.hasDiseaseEvidence(vision));
	}

	@Test
	void hasPestEvidence_ignoresNegatedPestClauses() {
		String text = "No insects, webbing, or scale seen after scanning.";
		assertFalse(VisualEvidencePatterns.hasPestEvidence(text));
	}

	@Test
	void hasPestEvidence_doesNotTreatLesionsAsPests() {
		String text = "Dark necrotic lesions with lighter centers and yellow halos.";
		assertFalse(VisualEvidencePatterns.hasPestEvidence(text));
		assertTrue(VisualEvidencePatterns.hasDiseaseEvidence(text));
	}

	@Test
	void claimsNoProblems_detectsFalseHealthyLanguage() {
		assertTrue(VisualEvidencePatterns.claimsNoProblems(
				"No visible symptoms — leaves appear healthy with no pests", "", ""));
		assertTrue(VisualEvidencePatterns.claimsNoProblems(
				"None visible — no leaf spots, necrosis, discoloration, pest activity", "", ""));
	}

	@Test
	void variegationAlone_isNotCredibleDiseaseEvidence() {
		String vision = VisionTestFixtures.COMPLETE_HEALTHY_POTHOS;
		assertFalse(VisualEvidencePatterns.hasCredibleDiseaseEvidence(vision));
		assertFalse(VisualEvidencePatterns.hasCredibleAbnormalityEvidence(vision));
		assertTrue(VisualEvidencePatterns.supportsHealthyConclusion(vision,
				"None visible — no leaf spots, necrosis, or pest activity", "Continue care"));
	}

	@Test
	void definiteLesions_remainCredibleDiseaseEvidence() {
		String vision = "Dark necrotic lesions with lighter centers and yellow halos.";
		assertTrue(VisualEvidencePatterns.hasCredibleDiseaseEvidence(vision));
		assertTrue(VisualEvidencePatterns.hasCredibleAbnormalityEvidence(vision));
	}
}
