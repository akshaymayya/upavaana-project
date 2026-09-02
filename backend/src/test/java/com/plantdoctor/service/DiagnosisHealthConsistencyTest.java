package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DiagnosisHealthConsistencyTest {

	@Test
	void coerce_whenHealthyButSymptomsDescribeDamage() {
		DiagnosisResult input = new DiagnosisResult(
				"Pothos",
				"Healthy",
				"Large holes and tears across the leaf indicating pest damage",
				"Continue care",
				"High — looks fine",
				true);
		DiagnosisResult out = DiagnosisHealthConsistency.enforce(input);
		assertEquals("Unidentified Issue", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
		assertEquals(input.symptoms_matched(), out.symptoms_matched());
		assertEquals("Pothos", out.plant_name());
	}

	@Test
	void coerce_whenHealthyButVisionShowsSevereLesions() {
		String vision = "Unknown plant. Numerous dark necrotic lesions with tan centers, extensive yellowing "
				+ "and damaged tissue across multiple leaves.";
		DiagnosisResult input = new DiagnosisResult(
				"Unknown Plant",
				"Healthy",
				"No visible symptoms",
				"Continue current care and maintenance.",
				"High — no symptoms observed",
				true);

		DiagnosisResult out = DiagnosisHealthConsistency.enforce(input, vision);

		assertEquals(Boolean.FALSE, out.is_healthy());
		assertNotEquals("Healthy", out.disease_name());
		assertFalse(out.symptomsEvidenceText().toLowerCase().contains("no visible symptoms"));
		assertFalse(VisualEvidencePatterns.suggestsHealthyCare(out.solution()));
		assertTrue(out.confidence_note().toLowerCase().contains("high")
				|| out.confidence_note().toLowerCase().contains("abnormal"));
	}

	@Test
	void coerce_whenHealthyDeniedSymptomsButVisionShowsYellowing() {
		String vision = "Herbaceous plant with significant yellowing and brown lesions; cause uncertain.";
		DiagnosisResult input = new DiagnosisResult(
				"Unknown",
				"Healthy",
				"No visible symptoms",
				"Continue care",
				"High",
				true);

		DiagnosisResult out = DiagnosisHealthConsistency.enforce(input, vision);

		assertNotEquals("Healthy", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
	}

	@Test
	void leaveHealthy_whenVisionAlsoSupportsHealth() {
		DiagnosisResult input = new DiagnosisResult(
				"Snake plant",
				"Healthy",
				"none",
				"Continue current care",
				"High",
				true);
		String vision = VisionTestFixtures.COMPLETE_HEALTHY_ASSESSMENT;
		DiagnosisResult out = DiagnosisHealthConsistency.enforce(input, vision);
		assertEquals("Healthy", out.disease_name());
		assertEquals(Boolean.TRUE, out.is_healthy());
	}

	@Test
	void insufficientImage_notAutomaticallyHealthy() {
		String vision = "Image too blurry to assess tissue detail; no reliable symptom read.";
		DiagnosisResult input = new DiagnosisResult(
				"Unknown", "Healthy", "none", "Continue care", "High", true);

		DiagnosisResult out = DiagnosisHealthConsistency.enforce(input, vision);

		assertNotEquals("Healthy", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
	}
}
