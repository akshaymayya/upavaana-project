package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.plantdoctor.entity.Disease;
import com.plantdoctor.entity.Plant;
import com.plantdoctor.service.DiseaseCandidate.MatchType;
import com.plantdoctor.service.VisualDiagnosisSupport.Category;

class DiagnosisEvidenceMappingTest {

	private static DiagnosisResult input(String disease, String symptoms, String solution, String note) {
		return new DiagnosisResult("Ficus", disease, symptoms, solution, note, false);
	}

	@Test
	void clearScaleInfestation_weakKb_mapsToSupportedPestCategory() {
		String vision = "Ficus leaf. Heavy scale insect colonies observed along leaf midrib.";
		DiagnosisResult in = input(
				"Unidentified Issue",
				"Heavy scale insect colonies observed along leaf midrib; no chewing damage",
				"Apply **horticultural oil** per the product label.",
				"Medium — symptom pattern only");

		DiseaseCandidate symptomOnly = symptomCandidate("Aloe Vera", "Leaf Spot", "brown spots");
		DiagnosisResult out = DiagnosisEvidenceMapping.enforce(in, vision, List.of(symptomOnly));

		assertEquals("Scale insect infestation", out.disease_name());
		assertTrue(out.confidence_note().contains("Diagnostic:"));
		assertTrue(out.confidence_note().contains("Retrieval:"));
		assertFalse(out.confidence_note().toLowerCase().contains("unidentified issue"));
	}

	@Test
	void clearAphidInfestation_weakKb_mapsToAphidCategory() {
		String vision = "Visible aphid clusters on leaf undersides.";
		DiagnosisResult in = input("Unidentified Issue", "Clusters of aphids on undersides",
				"Use insecticidal soap per label.", "Low");

		DiagnosisResult out = DiagnosisEvidenceMapping.enforce(in, vision, List.of());

		assertEquals("Aphid infestation", out.disease_name());
		assertEquals(Category.APHID_INFESTATION, VisualDiagnosisSupport.classify(vision, out.symptomsEvidenceText()));
	}

	@Test
	void clearWhiteflyInfestation_weakKb_mapsToWhiteflyCategory() {
		String vision = "Numerous whiteflies on the leaf surface.";
		DiagnosisResult in = input("Unidentified Issue", "Whiteflies visible on foliage",
				"Treat with horticultural oil per label.", "Low");

		DiagnosisResult out = DiagnosisEvidenceMapping.enforce(in, vision, List.of());

		assertEquals("Whitefly infestation", out.disease_name());
	}

	@Test
	void diseaseLesions_noPestEvidence_mapsToLeafSpotNotPest() {
		String vision = "Dark necrotic lesions with lighter centers; no insects visible.";
		DiagnosisResult in = input(
				"Unidentified Issue",
				"Dark brown spots with lighter centers and yellow halos",
				"Remove affected leaves and improve airflow.",
				"Medium");

		DiagnosisResult out = DiagnosisEvidenceMapping.enforce(in, vision, List.of());

		assertEquals("Leaf-spot disease", out.disease_name());
		assertFalse(VisualDiagnosisSupport.isPestCategory(
				VisualDiagnosisSupport.classify(vision, out.symptomsEvidenceText())));
	}

	@Test
	void ambiguousPestStructures_staysUnidentified() {
		String vision = "Small bumps could be scale insects or resin — uncertain.";
		DiagnosisResult in = input("Unidentified Issue", "Unclear bumps along midrib",
				"Monitor and photograph closer.", "Low");

		DiagnosisResult out = DiagnosisEvidenceMapping.enforce(in, vision, List.of());

		assertEquals(DiagnosisHealthConsistency.UNIDENTIFIED, out.disease_name());
		assertTrue(out.confidence_note().toLowerCase().contains("uncertain")
				|| out.confidence_note().toLowerCase().contains("low"));
	}

	@Test
	void healthyPlant_unchanged() {
		DiagnosisResult healthy = new DiagnosisResult(
				"Snake plant", "Healthy", "none", "Continue care", "High", true);
		assertSame(healthy, DiagnosisEvidenceMapping.enforce(healthy, VisionTestFixtures.COMPLETE_HEALTHY_ASSESSMENT, List.of()));
	}

	@Test
	void strongVisualAndStrongKbPlantMatch_usesKbDiseaseName() {
		String vision = "Ficus with heavy scale insect colonies on the midrib.";
		Plant ficus = new Plant("Ficus");
		Disease scale = new Disease(ficus, "Scale insects", "Armored scale", "scale colonies on leaves", "pests",
				"horticultural oil");
		List<DiseaseCandidate> candidates = List.of(new DiseaseCandidate(scale, MatchType.PLANT_NAME));

		DiagnosisResult in = input("Unidentified Issue", "Scale colonies on midrib",
				"Apply horticultural oil per label.", "Medium");

		DiagnosisResult out = DiagnosisEvidenceMapping.enforce(in, vision, candidates);

		assertEquals("Scale insects", out.disease_name());
		assertTrue(out.confidence_note().contains("Retrieval: High"));
	}

	@Test
	void inventedSevenToTenDaySchedule_isStripped() {
		DiagnosisResult in = input(
				"Scale insect infestation",
				"Scale colonies on midrib",
				"Apply horticultural oil. Repeat treatment in 7-10 days if scales remain.",
				"Medium");

		DiagnosisResult out = DiagnosisEvidenceMapping.enforce(in, "Scale colonies observed.", List.of());

		assertFalse(out.solution().toLowerCase().contains("7-10"));
		assertTrue(out.solution().toLowerCase().contains("label"));
	}

	private static DiseaseCandidate symptomCandidate(String plantName, String diseaseName, String symptoms) {
		Plant plant = new Plant(plantName);
		Disease disease = new Disease(plant, diseaseName, "desc", symptoms, "cause", "fix");
		return new DiseaseCandidate(disease, MatchType.SYMPTOM_PATTERN);
	}
}
