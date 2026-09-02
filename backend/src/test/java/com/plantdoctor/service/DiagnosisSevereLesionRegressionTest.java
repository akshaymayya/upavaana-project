package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.plantdoctor.config.DiagnosisProperties;
import com.plantdoctor.entity.Disease;
import com.plantdoctor.entity.Plant;
import com.plantdoctor.repository.DiseaseRepository;
import com.plantdoctor.repository.QueryRepository;
import com.plantdoctor.service.DiseaseCandidate.MatchType;

@ExtendWith(MockitoExtension.class)
class DiagnosisSevereLesionRegressionTest {

	@Mock
	private DiseaseRepository diseaseRepository;
	@Mock
	private QueryRepository queryRepository;
	@Mock
	private PlantVisionClient plantVisionClient;
	@Mock
	private NvidiaClientService nvidiaClientService;

	private DiagnosisService diagnosisService;

	@BeforeEach
	void setUp() {
		diagnosisService = new DiagnosisService(
				diseaseRepository, queryRepository, plantVisionClient, nvidiaClientService, new DiagnosisProperties());
	}

	@Test
	void extensiveHolesAndRaggedMargins_unknownPlant_notHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 });

		String vision = "Unknown ornamental. Morphology shows large missing sections, numerous irregular holes, "
				+ "and ragged leaf margins. TISSUE DAMAGE: none seen after scanning. "
				+ "PEST SURFACE SCAN: none seen. DISEASE SIGNS: none seen. ABIOTIC STRESS: none seen. "
				+ "OVERALL CONCLUSION: healthy.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Unknown Plant",
						"Healthy",
						"No visible symptoms",
						"Continue current care and maintenance.",
						"High — no symptoms",
						true));

		DiagnosisResult result = diagnosisService.diagnosePlant(file);

		assertNotEquals("Healthy", result.disease_name());
		assertEquals(Boolean.FALSE, result.is_healthy());
		assertEquals("Physical leaf damage", result.disease_name());
		assertFalse(result.solution().toLowerCase().contains("neem"));
		assertFalse(result.solution().toLowerCase().contains("horticultural oil"));
	}

	@Test
	void severeLesions_unknownPlant_weakKb_notHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 });

		String vision = "Unknown plant species. TISSUE DAMAGE: numerous dark brown/black lesions with tan centers, "
				+ "extensive necrotic tissue and yellowing across multiple leaves. "
				+ "PEST SURFACE SCAN: none seen. DISEASE SIGNS: severe leaf-spot-like lesions. OVERALL: not healthy.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Unknown Plant",
						"Healthy",
						"No visible symptoms",
						"Continue current care and maintenance.",
						"High — no symptoms",
						true));

		DiagnosisResult result = diagnosisService.diagnosePlant(file);

		assertNotEquals("Healthy", result.disease_name());
		assertEquals(Boolean.FALSE, result.is_healthy());
		assertFalse(result.symptomsEvidenceText().toLowerCase().contains("no visible symptoms"));
		assertFalse(VisualEvidencePatterns.suggestsHealthyCare(result.solution()));
		assertFalse(result.solution().toLowerCase().contains("neem"));
	}

	@Test
	void severeLesions_withKbSymptomPattern_mapsDiseaseNotHealthy() throws Exception {
		Plant aloe = new Plant("Aloe Vera");
		Disease leafSpot = new Disease(aloe, "Leaf Spot", "Fungal lesions",
				"Brown or black spots on leaves, sometimes with yellow halos.", "fungus", "remove leaves");
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of(leafSpot));

		String vision = "Unknown shrub. Dark lesions with yellow halos and necrotic tissue on multiple leaves.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Unknown Plant",
						"Healthy",
						"No visible symptoms",
						"Continue care",
						"High",
						true));

		DiagnosisResult result = diagnosisService.diagnosePlant(
				new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 }));

		assertNotEquals("Healthy", result.disease_name());
		assertTrue(result.disease_name().toLowerCase().contains("leaf")
				|| result.disease_name().toLowerCase().contains("unidentified"));
	}

	@Test
	void scaleInfestation_regression_stillNotHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		String vision = "Ficus. Heavy scale insect colonies along midrib.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Ficus", "Healthy", "No visible symptoms", "Continue care", "High", true));

		DiagnosisResult result = diagnosisService.diagnosePlant(
				new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 }));

		assertNotEquals("Healthy", result.disease_name());
		assertTrue(result.disease_name().toLowerCase().contains("scale")
				|| result.disease_name().toLowerCase().contains("pest"));
	}

	@Test
	void leafSpot_regression_noUnsupportedInsecticide() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		String vision = "Tomato leaf with dark necrotic lesions and lighter centers; no insects visible.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Tomato",
						"Unidentified Issue",
						"Dark spots with lighter centers",
						"Spray neem oil twice a week.",
						"Medium",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(
				new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 }));

		assertFalse(result.solution().toLowerCase().contains("neem"));
		assertNotEquals("Healthy", result.disease_name());
	}

	@Test
	void genuinelyHealthyPlant_staysHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		String vision = VisionTestFixtures.COMPLETE_HEALTHY_ASSESSMENT;
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Snake plant", "Healthy", "none", "Continue current care", "High", true));

		DiagnosisResult result = diagnosisService.diagnosePlant(
				new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 }));

		assertEquals("Healthy", result.disease_name());
		assertEquals(Boolean.TRUE, result.is_healthy());
	}

	@Test
	void strongKbPlantMatch_withLesions_usesKbDisease() throws Exception {
		Plant tomato = new Plant("Tomato");
		Disease earlyBlight = new Disease(tomato, "Early blight", "Fungal",
				"Dark lesions with concentric rings and yellow halos", "fungus", "remove leaves");
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of(earlyBlight));

		String vision = "Tomato plant with concentric dark lesions and yellow halos on leaves.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Tomato",
						"Unidentified Issue",
						"Concentric dark lesions with yellow halos",
						"Remove affected leaves.",
						"Medium",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(
				new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 }));

		assertEquals("Leaf-spot disease", result.disease_name());
	}

	@Test
	void ambiguousAbnormality_notHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		String vision = "Houseplant with some unusual discoloration on one leaf; cause unclear.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Unknown", "Healthy", "none", "Continue care", "High", true));

		DiagnosisResult result = diagnosisService.diagnosePlant(
				new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 }));

		assertNotEquals("Healthy", result.disease_name());
		assertEquals(Boolean.FALSE, result.is_healthy());
	}

	@Test
	void poorImageQuality_notAutomaticallyHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		String vision = "Image too blurry to assess tissue detail; no reliable symptom read.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Unknown", "Healthy", "none", "Continue care", "High", true));

		DiagnosisResult result = diagnosisService.diagnosePlant(
				new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 }));

		assertNotEquals("Healthy", result.disease_name());
		assertEquals(Boolean.FALSE, result.is_healthy());
	}

	@Test
	void variegatedHealthyPlant_kbDiseaseMatch_notLeafSpot() throws Exception {
		Plant pothos = new Plant("Epipremnum aureum");
		Disease leafSpot = new Disease(pothos, "Leaf Spot", "Fungal", "brown spots on leaves", "fungus", "remove");
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of(leafSpot));

		String vision = VisionTestFixtures.COMPLETE_HEALTHY_POTHOS;
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Epipremnum aureum",
						"Leaf-spot disease",
						"None visible — no leaf spots, necrosis, discoloration, pest activity, or waterlogging detected",
						"Visible leaf damage is present — this is not a healthy plant.",
						"Medium",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(
				new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1 }));

		assertEquals("Healthy", result.disease_name());
		assertEquals(Boolean.TRUE, result.is_healthy());
		assertFalse(result.solution().toLowerCase().contains("leaf damage"));
		assertFalse(result.symptomsEvidenceText().toLowerCase().contains("necrotic lesions"));
	}
}
