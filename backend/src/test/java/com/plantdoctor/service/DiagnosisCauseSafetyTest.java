package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DiagnosisCauseSafetyTest {

	@Test
	void fungalLookingSpots_notTreatedAsInsects() {
		DiagnosisResult input = new DiagnosisResult(
				"Tomato",
				"Insect damage",
				"Numerous dark necrotic lesions with lighter centers and yellowing",
				"Spray **neem oil spray** twice a week on all leaves.",
				"High — damage on leaf",
				false);
		DiagnosisResult out = DiagnosisCauseSafety.enforce(input);
		assertFalse(out.solution().toLowerCase().contains("neem"));
		assertFalse(out.solution().toLowerCase().contains("twice a week"));
		assertTrue(out.confidence_note().toLowerCase().contains("leaf-spot")
				|| out.confidence_note().toLowerCase().contains("lesion"));
		assertFalse(out.disease_name().toLowerCase().contains("insect"));
	}

	@Test
	void genuineInsectDamage_keepsPestTreatment() {
		DiagnosisResult input = new DiagnosisResult(
				"Hibiscus",
				"Chewing pest",
				"Ragged chew marks and feeding holes; caterpillar frass on the leaf",
				"Apply **insecticidal soap** on chewing insects.\nFollow the **product label**.",
				"Medium — chew pattern visible",
				false);
		DiagnosisResult out = DiagnosisCauseSafety.enforce(input);
		assertSame(input, out);
		assertTrue(out.solution().toLowerCase().contains("insecticidal"));
	}

	@Test
	void nutrientDeficiency_notInsecticide() {
		DiagnosisResult input = new DiagnosisResult(
				"Money plant",
				"Nutrient deficiency",
				"Interveinal yellowing on older leaves; no lesions or insects",
				"**Neem oil spray** for damaged leaves.",
				"Medium",
				false);
		DiagnosisResult out = DiagnosisCauseSafety.enforce(input);
		assertFalse(out.solution().toLowerCase().contains("neem"));
		assertTrue(out.solution().toLowerCase().contains("nutrient")
				|| out.solution().toLowerCase().contains("fertiliz")
				|| out.solution().toLowerCase().contains("environment"));
	}

	@Test
	void abioticScorch_notInsecticide() {
		DiagnosisResult input = new DiagnosisResult(
				"Succulent",
				"Sun scorch",
				"Crisp brown tips and bleached patches on sun-facing leaves; no insects",
				"**Neem oil spray** twice a week.",
				"High",
				false);
		DiagnosisResult out = DiagnosisCauseSafety.enforce(input);
		assertFalse(out.solution().toLowerCase().contains("neem"));
	}

	@Test
	void ambiguous_keepsUnidentifiedAndAsksForPhotos() {
		DiagnosisResult input = new DiagnosisResult(
				"Unknown",
				"Unidentified Issue",
				"Mild yellowing; photo is distant and blurry",
				"General care: check water and light.",
				"Low — limited view",
				false);
		DiagnosisResult out = DiagnosisCauseSafety.enforce(input);
		assertEquals("Unidentified Issue", out.disease_name());
		assertTrue(out.confidence_note().toLowerCase().contains("low")
				|| out.confidence_note().toLowerCase().contains("photo"));
	}

	@Test
	void multipleProblems_noteAllowedInConfidence() {
		DiagnosisResult input = new DiagnosisResult(
				"Rose",
				"Leaf-spot disease",
				"Discrete dark lesions with pale centers; also webbing and visible mites on the underside",
				"Remove badly spotted leaves.\nAlso treat **mites** per **product label**.",
				"Medium — lesions plus mites may both be present",
				false);
		DiagnosisResult out = DiagnosisCauseSafety.enforce(input);
		assertTrue(out.solution().toLowerCase().contains("mite") || out.confidence_note().toLowerCase().contains("mite"));
	}

	@Test
	void insufficientEvidence_requestsMorePhotos() {
		DiagnosisResult input = new DiagnosisResult(
				"Hibiscus",
				"Unidentified Issue",
				"One distant leaf, symptoms unclear",
				"Wait and see.",
				"High — sure",
				false);
		DiagnosisResult out = DiagnosisCauseSafety.enforce(input);
		assertTrue(out.confidence_note().toLowerCase().contains("low")
				|| out.confidence_note().toLowerCase().contains("close-up")
				|| out.confidence_note().toLowerCase().contains("underside"));
	}

	@Test
	void treatmentMatchesFungalCause() {
		DiagnosisResult input = new DiagnosisResult(
				"Tomato",
				"Unidentified Issue",
				"Scattered circular lesions with pale centers and yellow halos",
				"Remove badly affected leaves.\nReduce **leaf wetness**; follow any **product label** for disease control.",
				"Medium — lesion pattern, not feeding holes",
				false);
		DiagnosisResult out = DiagnosisCauseSafety.enforce(input);
		assertEquals(input.solution(), out.solution());
		assertFalse(out.solution().toLowerCase().contains("neem"));
	}

	@Test
	void weakEvidence_reducesHighConfidence() {
		DiagnosisResult input = new DiagnosisResult(
				"Bean",
				"Unidentified Issue",
				"Slight discoloration, image poorly lit",
				"Monitor the plant.",
				"High — definite blight",
				false);
		DiagnosisResult out = DiagnosisCauseSafety.enforce(input);
		assertTrue(out.confidence_note().toLowerCase().contains("low")
				|| out.confidence_note().toLowerCase().contains("medium"));
		assertFalse(out.confidence_note().toLowerCase().startsWith("High"));
	}
}
