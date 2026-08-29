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

	private DiagnosisProperties diagnosisProperties;

	private DiagnosisService diagnosisService;

	@BeforeEach
	void setUp() {
		diagnosisProperties = new DiagnosisProperties();
		diagnosisService = new DiagnosisService(diseaseRepository, queryRepository, nvidiaClientService,
				diagnosisProperties);
	}

	@Test
	void testDiagnosePlant_DefaultProviderCallsDeepSeekNotOpenAi() throws Exception {
		Plant moneyPlant = new Plant("Money Plant");
		moneyPlant.setCommonNames("Devil's Ivy,Pothos");
		Disease rootRot = new Disease(moneyPlant, "Root Rot", "Fungal disease", "Yellowing leaves, mushy roots",
				"Overwatering", "Reduce water");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of(rootRot));

		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

		when(nvidiaClientService.analyzeImage(any(), any()))
				.thenReturn("This plant looks like a money plant with yellowing leaves.");
		when(nvidiaClientService.synthesizeDiagnosisWithDeepSeek(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Money Plant", "Root Rot", "Yellowing leaves", "Reduce water", "High",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertNotNull(result);
		assertEquals("Money Plant", result.plant_name());
		verify(nvidiaClientService).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithGroq(anyString(), anyList());
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
		when(nvidiaClientService.analyzeImage(any(), any()))
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

		when(nvidiaClientService.analyzeImage(any(), any()))
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
	void testDiagnosePlant_OpenAiProviderCallsOpenAiNotDeepSeek() throws Exception {
		diagnosisProperties.setActiveSynthesisProvider("openai");

		Plant moneyPlant = new Plant("Money Plant");
		Disease rootRot = new Disease(moneyPlant, "Root Rot", "Fungal disease", "Yellowing leaves, mushy roots",
				"Overwatering", "Reduce water");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of(rootRot));

		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

		when(nvidiaClientService.analyzeImage(any(), any()))
				.thenReturn("This plant looks like a money plant with yellowing leaves.");
		when(nvidiaClientService.synthesizeDiagnosisWithOpenAi(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Money Plant", "Root Rot", "Yellowing leaves", "Reduce water", "High",
						false));

		DiagnosisResult result = diagnosisService.diagnosePlant(mockFile);

		assertNotNull(result);
		verify(nvidiaClientService).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithGroq(anyString(), anyList());
	}

	@Test
	void testDiagnosePlant_UnknownProviderUsesDeepSeek() throws Exception {
		diagnosisProperties.setActiveSynthesisProvider("foo");

		when(diseaseRepository.findAllWithPlant()).thenReturn(List.of());
		MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(nvidiaClientService.analyzeImage(any(), any())).thenReturn("A healthy succulent.");
		when(nvidiaClientService.synthesizeDiagnosisWithDeepSeek(anyString(), anyList()))
				.thenReturn(new DiagnosisResult("Succulent", "Healthy", "none", "Continue care", "High", true));

		diagnosisService.diagnosePlant(mockFile);

		verify(nvidiaClientService).synthesizeDiagnosisWithDeepSeek(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithGroq(anyString(), anyList());
		verify(nvidiaClientService, never()).synthesizeDiagnosisWithOpenAi(anyString(), anyList());
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

}
