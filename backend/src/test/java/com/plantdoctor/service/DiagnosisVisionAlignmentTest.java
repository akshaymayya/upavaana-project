package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DiagnosisVisionAlignmentTest {

	@Test
	void visionReportsScale_synthesisClaimsHealthy_isCorrected() {
		String vision = "Woody shrub or tree branch. PEST SURFACE SCAN: numerous small oval scale-like insects "
				+ "attached along the midrib and leaf surface. Tissue largely green with no holes or yellowing.";
		DiagnosisResult bad = new DiagnosisResult(
				"Woody shrub or tree branch",
				"Unidentified Issue",
				"No visible symptoms — leaves appear healthy with no holes, discoloration, pests, or lesions",
				"Continue your current care routine.",
				"High — vision reported no visible symptoms",
				false);

		DiagnosisResult out = DiagnosisVisionAlignment.enforce(bad, vision);

		assertEquals(Boolean.FALSE, out.is_healthy());
		assertFalse(out.symptomsEvidenceText().toLowerCase().contains("no visible symptoms"));
		assertFalse(out.symptomsEvidenceText().toLowerCase().contains("appear healthy"));
		assertTrue(VisualEvidencePatterns.hasPestEvidence(out.symptomsEvidenceText())
				|| out.symptomsEvidenceText().toLowerCase().contains("scale")
				|| out.symptomsEvidenceText().toLowerCase().contains("attached"));
		assertFalse(VisualEvidencePatterns.suggestsHealthyCare(out.solution()));
		assertTrue(out.solution().toLowerCase().contains("horticultural oil")
				|| out.solution().toLowerCase().contains("infestation"));
	}

	@Test
	void visionReportsLesions_synthesisHealthy_isCorrected() {
		String vision = "Herbaceous leaf with dark necrotic lesions and yellow halos; no insects seen.";
		DiagnosisResult bad = new DiagnosisResult(
				"Plant",
				"Healthy",
				"No visible symptoms",
				"Continue current care",
				"High",
				true);

		DiagnosisResult out = DiagnosisVisionAlignment.enforce(bad, vision);

		assertEquals(Boolean.FALSE, out.is_healthy());
		assertNotEquals("Healthy", out.disease_name());
		assertFalse(VisualEvidencePatterns.claimsNoProblems(out.symptomsEvidenceText(), out.solution(), ""));
	}

	@Test
	void visionAndSynthesisAgreeOnPest_passesThrough() {
		String vision = "Visible aphid clusters on leaf undersides.";
		DiagnosisResult good = new DiagnosisResult(
				"Hibiscus",
				"Aphids",
				"Clusters of aphids on undersides",
				"Apply **insecticidal soap** per the product label.",
				"Medium — aphids visible",
				false);

		assertSame(good, DiagnosisVisionAlignment.enforce(good, vision));
	}

	@Test
	void uncertainPestStructures_doesNotClaimHealthyOrDefiniteId() {
		String vision = "Small pale bumps along the midrib could be scale insects or resin droplets — uncertain.";
		DiagnosisResult bad = new DiagnosisResult(
				"Shrub",
				"Healthy",
				"Leaves appear healthy",
				"Continue care",
				"High",
				true);

		DiagnosisResult out = DiagnosisVisionAlignment.enforce(bad, vision);

		assertEquals(Boolean.FALSE, out.is_healthy());
		assertEquals(DiagnosisHealthConsistency.UNIDENTIFIED, out.disease_name());
		assertTrue(out.confidence_note().toLowerCase().contains("uncertain")
				|| out.confidence_note().toLowerCase().contains("low"));
		assertFalse(out.solution().toLowerCase().contains("neem"));
	}

	@Test
	void leafSpotVision_noFalseHealthyClaimWhenSynthesisAlreadyDescribesDamage() {
		String vision = "Dark necrotic lesions with lighter centers; no insects or webbing visible.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Tomato",
				"Unidentified Issue",
				"Dark brown spots with lighter centers",
				"Remove affected leaves and improve airflow.",
				"Medium",
				false);

		assertSame(synthesis, DiagnosisVisionAlignment.enforce(synthesis, vision));
	}
}
