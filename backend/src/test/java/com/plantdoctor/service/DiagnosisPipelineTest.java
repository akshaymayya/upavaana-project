package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.plantdoctor.entity.Disease;
import com.plantdoctor.entity.Plant;
import com.plantdoctor.service.DiseaseCandidate.MatchType;

class DiagnosisPipelineTest {

	@Test
	void extensiveHolesMissingTissueRaggedMargins_notHealthy_notInventedPest() {
		String vision = "Houseplant leaf morphology: large missing sections of the blade, numerous irregular holes, "
				+ "and ragged margins. TISSUE DAMAGE: none seen after scanning. "
				+ "PEST SURFACE SCAN: none seen after scanning. DISEASE SIGNS: none seen. "
				+ "ABIOTIC STRESS: none seen. OVERALL CONCLUSION: healthy.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Unknown Plant", "Healthy", "No visible symptoms", "Continue current care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertNotEquals("Healthy", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
		assertEquals("Physical leaf damage", out.disease_name());
		assertFalse(out.solution().toLowerCase().contains("neem"));
		assertFalse(out.solution().toLowerCase().contains("horticultural oil"));
		assertTrue(out.symptomsEvidenceText().toLowerCase().contains("hole")
				|| out.symptomsEvidenceText().toLowerCase().contains("missing")
				|| out.symptomsEvidenceText().toLowerCase().contains("ragged"));
	}

	@Test
	void obviousDisease_synthesisHealthy_notHealthy() {
		String vision = "Numerous dark necrotic lesions with tan centers and extensive yellowing across multiple leaves.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Unknown Plant", "Healthy", "No visible symptoms", "Continue care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertNotEquals("Healthy", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
		assertFalse(VisualEvidencePatterns.claimsNoProblems(out.symptomsEvidenceText(), out.solution(), ""));
	}

	@Test
	void obviousPest_synthesisDeniesSymptoms_mapsPestCategory() {
		String vision = "Heavy scale insect colonies attached along the leaf midrib.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Shrub", "Unidentified Issue",
				"No visible symptoms — leaves appear healthy",
				"Continue your current care routine.", "High", false);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertEquals("Scale insect infestation", out.disease_name());
		assertTrue(out.symptomsEvidenceText().toLowerCase().contains("scale")
				|| out.symptomsEvidenceText().toLowerCase().contains("attached"));
	}

	@Test
	void diseaseLesions_noPestEvidence_noInsecticide() {
		String vision = "Dark necrotic lesions with lighter centers; no insects visible.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Tomato", "Unidentified Issue", "Dark spots with lighter centers",
				"Spray neem oil twice a week.", "Medium", false);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertFalse(out.solution().toLowerCase().contains("neem"));
		assertEquals("Leaf-spot disease", out.disease_name());
	}

	@Test
	void variegatedPlant_explicitNoAbnormality_healthyNotDisease() {
		String vision = "Epipremnum aureum (golden pothos). Normal yellow and green variegation across leaves. "
				+ "TISSUE DAMAGE: none seen. PEST SURFACE SCAN: none seen. DISEASE SIGNS: none seen. "
				+ "ABIOTIC STRESS: none seen. No leaf spots, necrosis, pest activity, or waterlogging detected.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Epipremnum aureum",
				"Leaf-spot disease",
				"None visible — no leaf spots, necrosis, discoloration, pest activity, or waterlogging detected",
				"Visible leaf damage is present — this is not a healthy plant.",
				"Medium",
				false);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertEquals("Healthy", out.disease_name());
		assertEquals(Boolean.TRUE, out.is_healthy());
		assertFalse(out.solution().toLowerCase().contains("not a healthy plant"));
		assertFalse(out.solution().toLowerCase().contains("leaf damage"));
	}

	@Test
	void healthyPlant_speciesWithManyKbDiseases_staysHealthy() {
		Plant pothos = new Plant("Epipremnum aureum");
		Disease leafSpot = new Disease(pothos, "Leaf Spot", "Fungal", "brown spots on leaves", "fungus", "remove");
		Disease rootRot = new Disease(pothos, "Root Rot", "Fungal", "wilting yellow leaves", "fungus", "dry soil");
		List<DiseaseCandidate> candidates = List.of(
				new DiseaseCandidate(leafSpot, MatchType.PLANT_NAME),
				new DiseaseCandidate(rootRot, MatchType.PLANT_NAME));

		String vision = VisionTestFixtures.COMPLETE_HEALTHY_POTHOS;
		DiagnosisResult synthesis = new DiagnosisResult(
				"Epipremnum aureum",
				"Leaf Spot",
				"No visible symptoms — foliage appears normal for a variegated cultivar",
				"Remove affected leaves.",
				"Medium — plant in KB",
				false);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, candidates);

		assertEquals("Healthy", out.disease_name());
		assertEquals(Boolean.TRUE, out.is_healthy());
	}

	@Test
	void weakVisual_strongKbPlantNameOnly_doesNotUpgradeDiagnosis() {
		Plant tomato = new Plant("Tomato");
		Disease blight = new Disease(tomato, "Early blight", "Fungal", "lesions", "fungus", "remove leaves");
		List<DiseaseCandidate> candidates = List.of(new DiseaseCandidate(blight, MatchType.PLANT_NAME));

		String vision = "Tomato plant visible; foliage mostly green, no clear lesions in this distant photo.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Tomato", "Early blight", "none", "Remove affected leaves.", "High", false);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, candidates);

		assertNotEquals("Healthy", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
		assertEquals(DiagnosisPipeline.UNIDENTIFIED, out.disease_name());
	}

	@Test
	void strongVisual_weakKb_notUnidentified() {
		String vision = "Visible aphid clusters on leaf undersides.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Hibiscus", "Unidentified Issue", "Clusters of aphids on undersides",
				"Use insecticidal soap per label.", "Low", false);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertEquals("Aphid infestation", out.disease_name());
		assertTrue(out.confidence_note().contains("Diagnostic:"));
	}

	@Test
	void ambiguousUncertainPest_staysUnidentified() {
		String vision = "Small bumps along the midrib could be scale insects or resin — uncertain.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Shrub", "Healthy", "Leaves appear healthy", "Continue care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertEquals(DiagnosisPipeline.UNIDENTIFIED, out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
	}

	@Test
	void genuinelyHealthy_unchanged() {
		String vision = VisionTestFixtures.COMPLETE_HEALTHY_ASSESSMENT;
		DiagnosisResult synthesis = new DiagnosisResult(
				"Snake plant", "Healthy", "none", "Continue current care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertEquals("Healthy", out.disease_name());
		assertEquals(Boolean.TRUE, out.is_healthy());
	}

	@Test
	void insufficientImage_notAutomaticallyHealthy() {
		String vision = "Image too blurry to assess tissue detail; no reliable symptom read.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Unknown", "Healthy", "none", "Continue care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertNotEquals("Healthy", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
	}

	@Test
	void inventedSchedule_strippedEvenWithPest() {
		String vision = "Visible aphids and sticky residue.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Hibiscus", "Aphids", "Aphids present",
				"Spray insecticidal soap twice a week.", "Medium", false);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertFalse(out.solution().toLowerCase().contains("twice"));
		assertTrue(out.solution().toLowerCase().contains("label"));
	}

	@Test
	void healthyPothos_healthySynthesis_kbScaleCandidates_staysHealthy() {
		Plant pothos = new Plant("Epipremnum aureum");
		Disease scale = new Disease(pothos, "Scale insects", "Armored scale",
				"Scale insects commonly affect Epipremnum along leaf veins", "pests", "horticultural oil");
		Disease leafSpot = new Disease(pothos, "Leaf Spot", "Fungal",
				"Brown leaf spots with yellow halos on pothos leaves", "fungus", "remove leaves");
		Disease mealybug = new Disease(pothos, "Mealybug", "Pest",
				"White cottony mealybug clusters on stems", "pests", "alcohol swab");
		List<DiseaseCandidate> candidates = List.of(
				new DiseaseCandidate(scale, MatchType.SYMPTOM_PATTERN),
				new DiseaseCandidate(leafSpot, MatchType.SYMPTOM_PATTERN),
				new DiseaseCandidate(mealybug, MatchType.SYMPTOM_PATTERN));

		String vision = "Epipremnum aureum (golden pothos). TISSUE DAMAGE: None seen. "
				+ "PEST SURFACE SCAN: None seen. DISEASE SIGNS: None seen. ABIOTIC STRESS: None seen. "
				+ "Yellow/cream/chartreuse blotches are NORMAL GENETIC VARIEGATION. "
				+ "No fungal spots, bacterial lesions, halos, mildew, or necrotic patches.";

		DiagnosisResult synthesis = new DiagnosisResult(
				"Epipremnum aureum",
				"Healthy",
				"none",
				"Continue current care and maintenance.",
				"High — all categories negative",
				true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, candidates);

		assertEquals("Healthy", out.disease_name());
		assertEquals(Boolean.TRUE, out.is_healthy());
		assertNotEquals("Scale insect infestation", out.disease_name());
	}

	@Test
	void classifyFromVision_ignoresSynthesisSymptomsWithScaleKeywords() {
		String vision = "Epipremnum aureum. TISSUE DAMAGE: None seen. PEST SURFACE SCAN: None seen. "
				+ "DISEASE SIGNS: None seen. ABIOTIC STRESS: None seen.";
		String pollutedSymptoms = "SYMPTOM_PATTERN_MATCH Scale insects attached along midrib per KB candidate";

		assertEquals(VisualDiagnosisSupport.Category.UNIDENTIFIED,
				VisualDiagnosisSupport.classify(vision, pollutedSymptoms));
		assertEquals(VisualDiagnosisSupport.Category.UNIDENTIFIED,
				VisualDiagnosisSupport.classifyFromVision(vision));
	}

	@Test
	void explicitNoPests_kbScaleCandidate_notScaleInfestation() {
		Plant plant = new Plant("Houseplant");
		Disease scale = new Disease(plant, "Scale", "Pest", "scale insects on leaves", "pests", "oil");
		String vision = "Houseplant. PEST SURFACE SCAN: None seen. DISEASE SIGNS: None seen. "
				+ "TISSUE DAMAGE: None seen. ABIOTIC STRESS: None seen.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Houseplant", "Healthy", "none", "Continue care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision,
				List.of(new DiseaseCandidate(scale, MatchType.SYMPTOM_PATTERN)));

		assertEquals("Healthy", out.disease_name());
	}

	@Test
	void explicitNoLesions_kbLeafSpotCandidate_notLeafSpotDisease() {
		Plant plant = new Plant("Houseplant");
		Disease leafSpot = new Disease(plant, "Leaf Spot", "Fungal", "dark leaf spots with halos", "fungus", "remove");
		String vision = "Houseplant. DISEASE SIGNS: None seen. PEST SURFACE SCAN: None seen. "
				+ "TISSUE DAMAGE: None seen. ABIOTIC STRESS: None seen.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Houseplant", "Healthy", "none", "Continue care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision,
				List.of(new DiseaseCandidate(leafSpot, MatchType.SYMPTOM_PATTERN)));

		assertEquals("Healthy", out.disease_name());
	}

	@Test
	void healthyVariegated_discolorationContextInSynthesis_staysHealthy() {
		String vision = "Epipremnum aureum. TISSUE DAMAGE: None seen. PEST SURFACE SCAN: None seen. "
				+ "DISEASE SIGNS: None seen. ABIOTIC STRESS: None seen. "
				+ "Yellow and cream variegation is normal genetic coloration.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Epipremnum aureum",
				"Healthy",
				"Mild yellowing and discoloration noted in synthesis context — variegated cultivar",
				"Continue current care and maintenance.",
				"High",
				true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertEquals("Healthy", out.disease_name());
		assertEquals(Boolean.TRUE, out.is_healthy());
	}

	@Test
	void clearScaleVision_kbScaleCandidate_mapsScaleInfestation() {
		Plant ficus = new Plant("Ficus");
		Disease scale = new Disease(ficus, "Scale insects", "Armored scale",
				"Scale insects commonly cluster along leaf veins", "pests", "horticultural oil");
		String vision = "Ficus. Heavy scale insect colonies observed along leaf midrib.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Ficus",
				"Unidentified Issue",
				"Heavy scale insect colonies observed along leaf midrib",
				"Apply horticultural oil per label.",
				"Medium",
				false);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision,
				List.of(new DiseaseCandidate(scale, MatchType.SYMPTOM_PATTERN)));

		assertEquals("Scale insect infestation", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
	}

	@Test
	void clearDiseaseVision_kbDiseaseCandidate_mapsDisease() {
		Plant tomato = new Plant("Tomato");
		Disease blight = new Disease(tomato, "Early blight", "Fungal",
				"Dark lesions with concentric rings and yellow halos", "fungus", "remove leaves");
		String vision = "Tomato leaf with dark necrotic lesions, tan centers, and yellow halos.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Tomato",
				"Unidentified Issue",
				"Dark necrotic lesions with tan centers",
				"Remove affected leaves.",
				"Medium",
				false);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision,
				List.of(new DiseaseCandidate(blight, MatchType.SYMPTOM_PATTERN)));

		assertEquals("Leaf-spot disease", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
	}

	@Test
	void obviousHoles_noVisibleInsect_notHealthy_physicalDamage() {
		String vision = "Broadleaf plant. TISSUE DAMAGE: multiple holes through the leaf blade, missing tissue, "
				+ "and irregular ragged margins. PEST SURFACE SCAN: none seen. DISEASE SIGNS: none seen. "
				+ "ABIOTIC STRESS: none seen.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Unknown plant", "Healthy", "none", "Continue current care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertNotEquals("Healthy", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
		assertTrue(out.disease_name().toLowerCase().contains("physical")
				|| out.disease_name().toLowerCase().contains("chewing")
				|| out.disease_name().equals(DiagnosisPipeline.UNIDENTIFIED));
		assertFalse(out.symptomsEvidenceText().equalsIgnoreCase("none"));
		assertFalse(out.solution().toLowerCase().contains("neem"));
		assertFalse(out.solution().toLowerCase().contains("apply insecticidal"));
		assertTrue(out.solution().toLowerCase().contains("not a healthy plant")
				|| out.solution().toLowerCase().contains("holes")
				|| out.solution().toLowerCase().contains("inspect"));
	}

	@Test
	void holesWithVisibleChewingInsect_mapsChewingPestDamage() {
		String vision = "Leaf with multiple feeding holes and a caterpillar visible on the underside.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Unknown", "Healthy", "none", "Continue care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertNotEquals("Healthy", out.disease_name());
		assertTrue(out.disease_name().toLowerCase().contains("chew")
				|| out.disease_name().toLowerCase().contains("pest"));
	}

	@Test
	void healthyLeafNoHoles_staysHealthy() {
		String vision = "Snake plant. TISSUE DAMAGE: none seen. PEST SURFACE SCAN: none seen. "
				+ "DISEASE SIGNS: none seen. ABIOTIC STRESS: none seen.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Snake plant", "Healthy", "none", "Continue care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertEquals("Healthy", out.disease_name());
		assertEquals(Boolean.TRUE, out.is_healthy());
	}

	@Test
	void diseaseLesionsWithoutHoles_notClassifiedAsPhysicalDamage() {
		String vision = "Tomato leaf with dark necrotic lesions and tan centers; no holes or perforations.";
		DiagnosisResult synthesis = new DiagnosisResult(
				"Tomato", "Healthy", "none", "Continue care", "High", true);

		DiagnosisResult out = DiagnosisPipeline.enforce(synthesis, vision, List.of());

		assertNotEquals("Healthy", out.disease_name());
		assertFalse(out.disease_name().toLowerCase().contains("physical leaf"));
		assertTrue(out.disease_name().toLowerCase().contains("leaf-spot")
				|| out.disease_name().toLowerCase().contains("disease")
				|| out.disease_name().equals(DiagnosisPipeline.UNIDENTIFIED));
	}

	@Test
	void incompleteMorphologyOnly_synthesisHealthy_notHealthy() {
		DiagnosisResult synthesis = new DiagnosisResult(
				"Unknown plant", "Healthy", "none", "Continue current care", "High", true);
		DiagnosisResult out = DiagnosisPipeline.enforce(
				synthesis, VisionTestFixtures.INCOMPLETE_MORPHOLOGY_ONLY, List.of());

		assertNotEquals("Healthy", out.disease_name());
		assertEquals(Boolean.FALSE, out.is_healthy());
		assertTrue(out.symptomsEvidenceText().toLowerCase().contains("missing")
				|| out.symptomsEvidenceText().toLowerCase().contains("incomplete")
				|| out.symptomsEvidenceText().toLowerCase().contains("required"));
	}

	@Test
	void missingTissueSection_unknown_notAbsent() {
		String vision = "Plant. PEST SURFACE SCAN: none seen. DISEASE SIGNS: none seen. ABIOTIC STRESS: none seen.";
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(vision);
		assertFalse(assessment.tissueSectionSeen());
		assertFalse(assessment.tissueDamageNegative());
		assertFalse(VisualEvidencePatterns.hasCrediblePhysicalDamageEvidence(vision));
	}

	@Test
	void visionUnavailable_neverHealthy() {
		VisionUnavailableException failure = new VisionUnavailableException(
				"gemini", VisionFailureKind.SERVICE_UNAVAILABLE, "503");
		DiagnosisResult unavailable = DiagnosisPipeline.buildVisionUnavailableResult(failure);
		DiagnosisResult enforced = DiagnosisPipeline.enforce(
				unavailable, "", List.of());

		assertEquals(DiagnosisPipeline.VISION_UNAVAILABLE, enforced.disease_name());
		assertEquals(Boolean.FALSE, enforced.is_healthy());
		assertTrue(VisionUnavailableMarkers.isUnavailableDiagnosis(enforced));
	}

	@Test
	void synthesisUnavailable_neverHealthy() {
		DiagnosisResult unavailable = DiagnosisPipeline.buildSynthesisUnavailableResult(
				"Snake plant. TISSUE DAMAGE: None seen.",
				new RuntimeException("Read timed out"));
		DiagnosisResult enforced = DiagnosisPipeline.enforce(
				unavailable, VisionTestFixtures.COMPLETE_HEALTHY_ASSESSMENT, List.of());

		assertEquals(DiagnosisPipeline.SYNTHESIS_UNAVAILABLE, enforced.disease_name());
		assertEquals(Boolean.FALSE, enforced.is_healthy());
		assertNotEquals("Healthy", enforced.disease_name());
		assertTrue(VisionUnavailableMarkers.isUnavailableDiagnosis(enforced));
	}
}
