package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;

import com.plantdoctor.config.DiagnosisProperties;
import com.plantdoctor.entity.Disease;
import com.plantdoctor.entity.Plant;
import com.plantdoctor.entity.Query;
import com.plantdoctor.repository.DiseaseRepository;
import com.plantdoctor.repository.QueryRepository;
import com.plantdoctor.service.DiseaseCandidate.MatchType;

@ExtendWith(MockitoExtension.class)
public class DiagnosisServiceTest {

	@Mock
	private DiseaseRepository diseaseRepository;

	@Mock
	private QueryRepository queryRepository;

	@Mock
	private NvidiaClientService nvidiaClientService;

	@Mock
	private PlantVisionClient plantVisionClient;

	private DiagnosisProperties diagnosisProperties;

	private DiagnosisService diagnosisService;

	@BeforeEach
	void setUp() {
		diagnosisProperties = new DiagnosisProperties();
		diagnosisService = new DiagnosisService(diseaseRepository, queryRepository, plantVisionClient,
				nvidiaClientService, diagnosisProperties);
	}

	@Test
	void testDiagnosePlant_DefaultProviderCallsGroqNotDeepSeekOrOpenAi() throws Exception {
		Plant moneyPlant = new Plant("Money Plant");
		moneyPlant.setCommonNames("Devil's Ivy,Pothos");
		Disease rootRot = new Disease(moneyPlant, "Root Rot", "Fungal disease", "Yellowing leaves, mushy roots",
				"Overwatering", "Reduce water");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of(rootRot));

		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

		when(plantVisionClient.analyzeImage(any(), any()))
				.thenReturn("This plant looks like a money plant with yellowing leaves.");
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Money Plant", "Root Rot", "Yellowing leaves", "Reduce water", "High",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertNotNull(result);
		assertEquals("Money Plant", result.plant_name());
		verify(nvidiaClientService).synthesizeDiagnosisWithGroq(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
		verify(queryRepository).save(any());
	}

	@Test
	void testDiagnosePlant_ActiveSynthesisProviderEnvVarGroqRoutesToGroq() throws Exception {
		MockEnvironment env = new MockEnvironment();
		env.setProperty(DiagnosisProperties.ENV_ACTIVE_SYNTHESIS_PROVIDER, "groq");
		diagnosisProperties.attachEnvironment(env);
		diagnosisProperties.setActiveSynthesisProvider("deepseek");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(plantVisionClient.analyzeImage(any(), any()))
				.thenReturn("This plant looks like a money plant with yellowing leaves.");
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Money Plant", "Root Rot", "Yellowing leaves", "Reduce water", "High",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertNotNull(result);
		verify(nvidiaClientService).synthesizeDiagnosisWithGroq(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
	}

	@Test
	void testDiagnosePlant_GroqProviderCallsGroqNotDeepSeekOrOpenAi() throws Exception {
		diagnosisProperties.setActiveSynthesisProvider("groq");

		Plant moneyPlant = new Plant("Money Plant");
		Disease rootRot = new Disease(moneyPlant, "Root Rot", "Fungal disease", "Yellowing leaves, mushy roots",
				"Overwatering", "Reduce water");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of(rootRot));

		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

		when(plantVisionClient.analyzeImage(any(), any()))
				.thenReturn("This plant looks like a money plant with yellowing leaves.");
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Money Plant", "Root Rot", "Yellowing leaves", "Reduce water", "High",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertNotNull(result);
		verify(nvidiaClientService).synthesizeDiagnosisWithGroq(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
	}

	@Test
	void testDiagnosePlant_OpenAiConfiguredStillUsesGroqOnly() throws Exception {
		diagnosisProperties.setActiveSynthesisProvider("openai");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(plantVisionClient.analyzeImage(any(), any()))
				.thenReturn("This plant looks like a money plant with yellowing leaves.");
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Money Plant", "Root Rot", "Yellowing leaves", "Reduce water", "High",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertNotNull(result);
		verify(nvidiaClientService).synthesizeDiagnosisWithGroq(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
	}

	@Test
	void testDiagnosePlant_DeepSeekConfiguredStillUsesGroqOnly() throws Exception {
		diagnosisProperties.setActiveSynthesisProvider("deepseek");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(plantVisionClient.analyzeImage(any(), any()))
				.thenReturn("This plant looks like a money plant with yellowing leaves.");
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Money Plant", "Root Rot", "Yellowing leaves", "Reduce water", "High",
						false));

		diagnosisService.diagnosePlant(mockFile);

		verify(nvidiaClientService).synthesizeDiagnosisWithGroq(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
	}

	@Test
	void testDiagnosePlant_UnknownProviderUsesGroq() throws Exception {
		diagnosisProperties.setActiveSynthesisProvider("foo");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn("A healthy succulent.");
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Succulent", "Healthy", "none", "Continue care", "High", true));

		diagnosisService.diagnosePlant(mockFile);

		verify(nvidiaClientService).synthesizeDiagnosisWithGroq(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
	}

	@Test
	void testDiagnosePlant_usesPlantVisionClientNotNvidiaVision() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn("Healthy snake plant, no symptoms.");
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Snake plant", "Healthy", "none", "Continue care", "High", true));

		diagnosisService.diagnosePlant(mockFile);

		verify(plantVisionClient).analyzeImage(any(), eq("image/jpeg"));
		verify(nvidiaClientService, never()).analyzeImage(any(), any());
	}

	@Test
	void testDiagnosePlant_healthyPlantRemainsHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(plantVisionClient.analyzeImage(any(), any()))
				.thenReturn(VisionTestFixtures.COMPLETE_HEALTHY_ASSESSMENT);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Snake plant", "Healthy", "none", "Continue current care", "High", true));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertEquals("Healthy", result.disease_name());
		assertEquals(Boolean.TRUE, result.is_healthy());
	}

	@Test
	void testDiagnosePlant_visionUnavailable_skipsSynthesisAndReturnsRetryState() throws Exception {
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(plantVisionClient.analyzeImage(any(), any()))
				.thenThrow(new VisionUnavailableException(
						"gemini", VisionFailureKind.SERVICE_UNAVAILABLE,
						"503 Service Unavailable — high demand"));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertEquals(DiagnosisPipeline.VISION_UNAVAILABLE, result.disease_name());
		assertEquals(Boolean.FALSE, result.is_healthy());
		assertNotEquals("Healthy", result.disease_name());
		assertTrue(result.solution().toLowerCase().contains("unable to analyze"));
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithGroq(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
		verify(queryRepository).save(any(Query.class));
	}

	@Test
	void testDiagnosePlant_visiblePestInfestation_notClassifiedHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

		String vision = "Woody shrub or tree branch. PEST SURFACE SCAN: numerous small oval scale-like insects "
				+ "attached along the midrib and leaf surface. No holes or yellowing in the tissue.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Woody shrub or tree branch",
						"Unidentified Issue",
						"No visible symptoms — leaves appear healthy with no holes, discoloration, pests, or lesions",
						"Continue your current care routine.",
						"High — vision reported no visible symptoms",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertEquals(Boolean.FALSE, result.is_healthy());
		assertFalse(result.symptomsEvidenceText().toLowerCase().contains("no visible symptoms"));
		assertFalse(VisualEvidencePatterns.suggestsHealthyCare(result.solution()));
		assertTrue(VisualEvidencePatterns.hasPestEvidence(result.symptomsEvidenceText())
				|| result.symptomsEvidenceText().toLowerCase().contains("scale")
				|| result.symptomsEvidenceText().toLowerCase().contains("attached"));
	}

	@Test
	void testDiagnosePlant_scaleInfestation_mapsSupportedDiagnosisNotUnidentified() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

		String vision = "Ficus. Heavy scale insect colonies observed along leaf midrib; no chewing damage or discoloration.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Ficus",
						"Unidentified Issue",
						"Heavy scale insect colonies observed along leaf midrib; no visible chewing damage or leaf discoloration.",
						"Apply horticultural oil (e.g., neem oil) to the affected foliage.\n"
								+ "Repeat treatment in 7-10 days if scales remain.",
						"Medium — symptom pattern only",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertNotEquals(DiagnosisHealthConsistency.UNIDENTIFIED, result.disease_name());
		assertTrue(result.disease_name().toLowerCase().contains("scale"));
		assertFalse(result.solution().toLowerCase().contains("7-10"));
		assertTrue(result.confidence_note().contains("Diagnostic:"));
		assertTrue(result.confidence_note().contains("Retrieval:"));
		assertEquals(Boolean.FALSE, result.is_healthy());
	}

	@Test
	void testDiagnosePlant_unsupportedInsecticideRewrittenForLesionPattern() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

		String vision = "Herbaceous plant in Solanaceae family. Dark necrotic lesions with lighter tan centers, "
				+ "surrounding yellowing across multiple leaves. No insects, webbing, or chewing margins visible.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult(
						"Herbaceous plant in Solanaceae family",
						"Unidentified Issue",
						"Small holes and pinholes on leaves, yellowing and wilting edges, dark brown spots with lighter centers",
						"Apply an organic insecticide such as neem oil to affected leaves.\n"
								+ "Spray neem oil twice a week and check for aphids or whiteflies.",
						"Medium",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertEquals("Leaf-spot disease", result.disease_name());
		assertFalse(result.solution().toLowerCase().contains("neem"));
		assertFalse(result.solution().toLowerCase().contains("insecticide"));
		assertFalse(result.solution().toLowerCase().contains("twice"));
		assertTrue(result.solution().toLowerCase().contains("leaf-spot")
				|| result.solution().toLowerCase().contains("airflow")
				|| result.solution().toLowerCase().contains("label"));
		assertTrue(result.confidence_note().toLowerCase().contains("pest")
				|| result.confidence_note().toLowerCase().contains("visual")
				|| result.confidence_note().toLowerCase().contains("leaf-spot"));
	}

	@Test
	void testFindCandidateDiseases_CapsPlantNameMatchesAtFive() {
		Plant moneyPlant = new Plant("Money Plant");
		List<Disease> diseases = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			diseases.add(new Disease(moneyPlant, "Disease " + i, "desc", "Yellowing leaves", "cause", "fix"));
		}
		when(diseaseRepository.findAllWithPlant()).thenReturn(diseases);

		List<DiseaseCandidate> candidates = diagnosisService.findCandidateDiseases("money plant yellowing leaves");

		long plantName = candidates.stream().filter(c -> c.matchType() == MatchType.PLANT_NAME).count();
		assertEquals(5, plantName);
	}

	@Test
	void testFindCandidateDiseases_SymptomPatternWhenPlantMisidentified() {
		Plant aloeVera = new Plant("Aloe Vera");
		Disease leafSpot = new Disease(aloeVera, "Leaf Spot", "Fungal or bacterial lesions",
				"Brown or black spots on leaves, sometimes with yellow halos around them.", "Humidity", "Remove leaves");

		Plant moneyPlant = new Plant("Money Plant");
		Disease rootRot = new Disease(moneyPlant, "Root Rot", "Fungal disease", "Yellowing leaves, mushy roots",
				"Overwatering", "Reduce water");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of(leafSpot, rootRot));

		String misidentifiedVision = "Possibly lettuce or spinach, herbaceous leaf with brown and black lesions "
				+ "surrounded by yellow halos on the leaf surface.";

		List<DiseaseCandidate> candidates = diagnosisService.findCandidateDiseases(misidentifiedVision);

		assertEquals(1, candidates.size());
		assertEquals(MatchType.SYMPTOM_PATTERN, candidates.get(0).matchType());
		assertEquals("Leaf Spot", candidates.get(0).disease().getDiseaseName());
		assertEquals("Aloe Vera", candidates.get(0).disease().getPlant().getName());
	}

	@Test
	void testFindCandidateDiseases_PlantMatchTakesPrecedenceOverSymptomPattern() {
		Plant aloeVera = new Plant("Aloe Vera");
		Disease leafSpot = new Disease(aloeVera, "Leaf Spot", "Fungal lesions",
				"Brown or black spots on leaves, sometimes with yellow halos around them.", "Humidity", "Remove leaves");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of(leafSpot));

		String vision = "Aloe vera succulent with brown spots and yellow halos on the leaves.";

		List<DiseaseCandidate> candidates = diagnosisService.findCandidateDiseases(vision);

		assertEquals(1, candidates.size());
		assertEquals(MatchType.PLANT_NAME, candidates.get(0).matchType());
		assertEquals("Leaf Spot", candidates.get(0).disease().getDiseaseName());
	}

	@Test
	void testDiagnosePlant_groqTimeout_returnsSynthesisUnavailableNotHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(plantVisionClient.analyzeImage(any(), any()))
				.thenReturn(VisionTestFixtures.COMPLETE_HEALTHY_ASSESSMENT);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenThrow(new RuntimeException("I/O error on POST request: Read timed out"));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertEquals(DiagnosisPipeline.SYNTHESIS_UNAVAILABLE, result.disease_name());
		assertEquals(Boolean.FALSE, result.is_healthy());
		assertNotEquals("Healthy", result.disease_name());
		assertTrue(VisionUnavailableMarkers.isUnavailableDiagnosis(result));
		verify(queryRepository).save(any(Query.class));
	}

	@Test
	void testDiagnosePlant_incompleteVision_notHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(plantVisionClient.analyzeImage(any(), any()))
				.thenReturn(VisionTestFixtures.INCOMPLETE_MORPHOLOGY_ONLY);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Unknown plant", "Healthy", "none", "Continue care", "High", true));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertNotEquals("Healthy", result.disease_name());
		assertEquals(Boolean.FALSE, result.is_healthy());
	}

	@Test
	void testDiagnosePlant_physicalDamage_notHealthy() throws Exception {
		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		String vision = "Broadleaf plant. TISSUE DAMAGE: multiple holes through the leaf blade, missing tissue. "
				+ "PEST SURFACE SCAN: none seen. DISEASE SIGNS: none seen. ABIOTIC STRESS: none seen.";
		when(plantVisionClient.analyzeImage(any(), any())).thenReturn(vision);
		when(nvidiaClientService.synthesizeDiagnosisWithGroq(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Unknown plant", "Healthy", "none", "Continue care", "High", true));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertNotEquals("Healthy", result.disease_name());
		assertEquals(Boolean.FALSE, result.is_healthy());
	}

}
