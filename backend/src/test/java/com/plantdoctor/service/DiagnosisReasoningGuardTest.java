package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.plantdoctor.entity.Disease;
import com.plantdoctor.entity.Plant;
import com.plantdoctor.service.DiseaseCandidate.MatchType;

class DiagnosisReasoningGuardTest {

	private static final String LESION_VISION =
			"Dark necrotic lesions with lighter centers and surrounding yellowing. No insects, webbing, or chewing margins seen.";

	private static DiagnosisResult result(String disease, String symptoms, String solution) {
		return new DiagnosisResult("Tomato", disease, symptoms, solution, "High", false);
	}

	@Test
	void fungalLesions_doNotBecomeInsecticide() {
		DiagnosisResult in = result("Unidentified Issue",
				"Dark lesions with lighter centers; yellowing",
				"Spray **neem oil spray** twice a week for insects.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, LESION_VISION, List.of());
		assertFalse(out.solution().toLowerCase().contains("neem"));
		assertFalse(out.solution().toLowerCase().contains("twice"));
		assertEquals("Unidentified Issue", out.disease_name());
		assertTrue(out.solution().toLowerCase().contains("leaf-spot") || out.solution().toLowerCase().contains("label"));
	}

	@Test
	void chewingMarksWithoutVisibleInsect_notConfirmedPestTreatment() {
		String vision = "Irregular chewing marks and ragged leaf margins. No fungal spots.";
		DiagnosisResult in = result("Chewing pest", "Ragged chewing marks",
				"Apply **neem oil spray** to remaining foliage.\nFollow the product label.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, vision, List.of());
		assertFalse(out.solution().toLowerCase().contains("neem"));
		assertNotEquals("Healthy", out.disease_name());
		assertTrue(out.solution().toLowerCase().contains("inspect")
				|| out.solution().toLowerCase().contains("not a healthy plant")
				|| out.solution().toLowerCase().contains("holes"));
	}

	@Test
	void chewingWithVisibleInsect_pestTreatmentRemains() {
		String vision = "Ragged chewing marks with a caterpillar visible on the leaf margin.";
		DiagnosisResult in = result("Chewing pest", "Chew marks and visible caterpillar",
				"Apply **neem oil spray** to remaining foliage.\nFollow the product label.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, vision, List.of());
		assertTrue(out.solution().toLowerCase().contains("neem"));
	}

	@Test
	void visibleAphids_pestSupported() {
		String vision = "Clusters of aphids on the undersides of leaves.";
		DiagnosisResult in = result("Aphids", "Visible aphids", "Use **insecticidal soap**. Follow the label.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, vision, List.of());
		assertTrue(out.solution().toLowerCase().contains("insecticidal"));
	}

	@Test
	void webbing_pestSupported() {
		String vision = "Fine webbing across leaf axils, consistent with mites.";
		DiagnosisResult in = result("Spider mites", "Webbing on leaves", "Treat with **neem**. Follow the label.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, vision, List.of());
		assertTrue(out.solution().toLowerCase().contains("neem"));
	}

	@Test
	void nutrientLike_noPesticide() {
		String vision = "Uniform yellowing of older leaves; no spots, holes, or insects.";
		DiagnosisResult in = result("Unidentified Issue", "Interveinal yellowing",
				"Spray neem oil twice a week.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, vision, List.of());
		assertFalse(out.solution().toLowerCase().contains("neem"));
	}

	@Test
	void abiotic_doesNotForceDiseaseName() {
		String vision = "Leaf scorch on sun-facing edges; no lesions or pests.";
		DiagnosisResult in = result("Unidentified Issue", "Crisp brown edges", "Increase shade; water evenly.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, vision, List.of());
		assertEquals("Unidentified Issue", out.disease_name());
		assertFalse(INSECTICIDE_WORD(out.solution()));
	}

	@Test
	void ambiguousSpots_confidenceMentionsLimitedVisual() {
		DiagnosisResult in = result("Unidentified Issue", "Scattered dark spots", "Improve airflow.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, LESION_VISION, List.of());
		assertTrue(out.confidence_note().toLowerCase().contains("retrieval"));
		assertTrue(out.confidence_note().toLowerCase().contains("visual"));
	}

	@Test
	void noKbCandidate_staysUnidentified() {
		DiagnosisResult in = result("Unidentified Issue", "Mixed marks", "General care; photograph closer.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, "Unclear specks on one leaf.", List.of());
		assertEquals("Unidentified Issue", out.disease_name());
		assertTrue(out.confidence_note().contains("Low"));
	}

	@Test
	void plantNameMatch_retrievalHighNotVisualCertainty() {
		Plant tomato = new Plant("Tomato");
		Disease leafSpot = new Disease(tomato, "Leaf Spot", "desc", "spots", "fungus", "remove leaves");
		List<DiseaseCandidate> cands = List.of(new DiseaseCandidate(leafSpot, MatchType.PLANT_NAME));
		DiagnosisResult in = result("Leaf Spot", "Spots on leaves", "Remove infected leaves. Follow any product label.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, LESION_VISION, cands);
		assertTrue(out.confidence_note().contains("High"));
		assertTrue(out.confidence_note().toLowerCase().contains("not the same"));
	}

	@Test
	void fungalDiagnosis_insecticideOnly_isRewritten() {
		Plant tomato = new Plant("Tomato");
		Disease blight = new Disease(tomato, "Early blight", "desc", "lesions", "fungus", "remove leaves");
		List<DiseaseCandidate> cands = List.of(new DiseaseCandidate(blight, MatchType.PLANT_NAME));
		DiagnosisResult in = result("Early blight", "Concentric lesions with yellow halos",
				"Spray neem oil twice a week.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, LESION_VISION, cands);
		assertEquals("Early blight", out.disease_name());
		assertFalse(out.solution().toLowerCase().contains("neem"));
		assertTrue(out.solution().toLowerCase().contains("label") || out.solution().toLowerCase().contains("airflow"));
	}

	@Test
	void genericNeemWithoutPestEvidence_removed() {
		DiagnosisResult in = result("Unidentified Issue", "Damaged leaves", "Use neem oil.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, "Brown patches, no insects.", List.of());
		assertFalse(out.solution().toLowerCase().contains("neem"));
	}

	@Test
	void inventedSchedule_strippedEvenWithPest() {
		String vision = "Visible aphids and sticky residue.";
		DiagnosisResult in = result("Aphids", "Aphids present",
				"Spray insecticidal soap twice a week.");
		DiagnosisResult out = DiagnosisReasoningGuard.enforce(in, vision, List.of());
		assertFalse(out.solution().toLowerCase().contains("twice"));
		assertTrue(out.solution().toLowerCase().contains("label"));
	}

	@Test
	void looksLikeLesionNotPest_helper() {
		assertTrue(DiagnosisReasoningGuard.looksLikeLesionNotPest(LESION_VISION));
		assertFalse(DiagnosisReasoningGuard.looksLikeLesionNotPest("Ragged chewing marks on the margin."));
	}

	private static boolean INSECTICIDE_WORD(String s) {
		return s.toLowerCase().contains("neem") || s.toLowerCase().contains("insecticide");
	}
}
