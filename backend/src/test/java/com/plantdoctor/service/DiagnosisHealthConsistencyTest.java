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
	void leaveHealthy_whenSymptomsAreNone() {
		DiagnosisResult input = new DiagnosisResult(
				"Snake plant",
				"Healthy",
				"none",
				"Continue current care",
				"High",
				true);
		DiagnosisResult out = DiagnosisHealthConsistency.enforce(input);
		assertEquals("Healthy", out.disease_name());
		assertEquals(Boolean.TRUE, out.is_healthy());
	}

	@Test
	void leaveUnidentified_whenAlreadyCorrect() {
		DiagnosisResult input = new DiagnosisResult(
				"Unknown",
				"Unidentified Issue",
				"Yellowing leaf edges",
				"Check watering",
				"Low",
				false);
		assertSame(input, DiagnosisHealthConsistency.enforce(input));
	}
}
