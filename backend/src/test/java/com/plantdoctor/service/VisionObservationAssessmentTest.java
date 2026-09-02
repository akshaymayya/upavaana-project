package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class VisionObservationAssessmentTest {

	@Test
	void structuredNegatives_allCategoriesNegative() {
		String vision = "Epipremnum aureum. TISSUE DAMAGE: None seen. PEST SURFACE SCAN: None seen. "
				+ "DISEASE SIGNS: None seen. ABIOTIC STRESS: None seen. "
				+ "Yellow/cream blotches are normal genetic variegation.";
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(vision);
		assertTrue(assessment.isAssessmentComplete(), assessment.diagnosticSummary());
		assertTrue(assessment.allCategoriesNegative(), assessment.diagnosticSummary());
		assertFalse(VisualEvidencePatterns.hasCrediblePestEvidence(vision));
		assertFalse(VisualEvidencePatterns.hasCredibleAbnormalityEvidence(vision));
		assertEquals(VisualDiagnosisSupport.Category.UNIDENTIFIED,
				VisualDiagnosisSupport.classifyFromVision(vision));
	}

	@Test
	void scanBoilerplateWithScaleKeyword_doesNotCreatePestEvidence() {
		String vision = "PEST SURFACE SCAN: Inspected midrib and veins for scale-like insects; none seen. "
				+ "DISEASE SIGNS: None seen. TISSUE DAMAGE: None seen. ABIOTIC STRESS: None seen.";
		assertFalse(VisualEvidencePatterns.hasCrediblePestEvidence(vision));
		assertFalse(VisualDiagnosisSupport.classifyFromVision(vision)
				== VisualDiagnosisSupport.Category.SCALE_INFESTATION);
	}

	@Test
	void clearScaleObservation_classifiesScale() {
		String vision = "PEST SURFACE SCAN: Numerous scale insects attached along the midrib.";
		assertTrue(VisualEvidencePatterns.hasCrediblePestEvidence(vision));
		assertEquals(VisualDiagnosisSupport.Category.SCALE_INFESTATION,
				VisualDiagnosisSupport.classifyFromVision(vision));
	}

	@Test
	void tissueDamagePresent_whenHolesDescribedButTissueSectionSaysNoneSeen() {
		String vision = "Broad leaf with large missing sections, numerous irregular holes, and ragged margins. "
				+ "TISSUE DAMAGE: none seen after scanning. PEST SURFACE SCAN: none seen after scanning. "
				+ "DISEASE SIGNS: none seen. ABIOTIC STRESS: none seen. OVERALL CONCLUSION: healthy.";
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(vision);
		assertTrue(assessment.tissueDamagePositive(), assessment.diagnosticSummary());
		assertEquals(VisionObservationAssessment.CategoryState.PRESENT, assessment.tissueDamageState());
		assertTrue(assessment.pestScanNegative());
		assertFalse(assessment.allCategoriesExplicitlyNegative());
		assertTrue(VisualEvidencePatterns.hasCrediblePhysicalDamageEvidence(vision));
		assertTrue(VisualEvidencePatterns.hasCredibleAbnormalityEvidence(vision));
		assertFalse(VisualEvidencePatterns.supportsHealthyConclusion(vision, "none", "Continue care"));
		assertEquals(VisualDiagnosisSupport.Category.PHYSICAL_LEAF_DAMAGE,
				VisualDiagnosisSupport.classifyFromVision(vision));
	}

	@Test
	void tissueDamagePositive_despiteNegativePestScan() {
		String vision = "Houseplant. TISSUE DAMAGE: multiple holes through the leaf blade and missing tissue along margins. "
				+ "PEST SURFACE SCAN: none seen. DISEASE SIGNS: none seen. ABIOTIC STRESS: none seen.";
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(vision);
		assertTrue(assessment.tissueDamagePositive());
		assertFalse(assessment.pestScanNegative() && assessment.allCategoriesNegative());
		assertTrue(VisualEvidencePatterns.hasCrediblePhysicalDamageEvidence(vision));
		assertTrue(VisualEvidencePatterns.hasCredibleAbnormalityEvidence(vision));
		assertEquals(VisualDiagnosisSupport.Category.PHYSICAL_LEAF_DAMAGE,
				VisualDiagnosisSupport.classifyFromVision(vision));
	}

	@Test
	void missingSections_areUnknown_notAbsent() {
		String vision = "Houseplant. PEST SURFACE SCAN: none seen. DISEASE SIGNS: none seen.";
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(vision);
		assertFalse(assessment.tissueSectionSeen());
		assertFalse(assessment.tissueDamageNegative());
		assertTrue(assessment.pestScanNegative());
		assertTrue(assessment.isIncompleteHealthAssessment());
		assertFalse(VisualEvidencePatterns.supportsHealthyConclusion(vision, "none", "Continue care"));
		assertFalse(VisualEvidencePatterns.hasCrediblePestEvidence(vision));
	}

	@Test
	void incompleteMorphologyOnly_notHealthy() {
		String vision = VisionTestFixtures.INCOMPLETE_MORPHOLOGY_ONLY;
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(vision);
		assertTrue(assessment.isIncompleteHealthAssessment());
		assertFalse(assessment.canUseFreeTextEvidence());
		assertFalse(VisualEvidencePatterns.supportsHealthyConclusion(vision, "none", "Continue care"));

		DiagnosisResult synthesis = new DiagnosisResult(
				"Unknown", "Healthy", "none", "Continue care", "High", true);
		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());
		assertNotEquals("Healthy", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
	}
}
