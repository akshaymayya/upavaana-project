package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plantdoctor.config.DiagnosisProperties;

@ExtendWith(MockitoExtension.class)
class RoutingPlantVisionClientTest {

	@Mock
	private GeminiPlantVisionClient geminiPlantVisionClient;

	@Mock
	private NvidiaPlantVisionClient nvidiaPlantVisionClient;

	private DiagnosisProperties diagnosisProperties;

	private RoutingPlantVisionClient routingPlantVisionClient;

	@BeforeEach
	void setUp() {
		diagnosisProperties = new DiagnosisProperties();
		routingPlantVisionClient = new RoutingPlantVisionClient(
				diagnosisProperties, geminiPlantVisionClient, nvidiaPlantVisionClient);
	}

	@Test
	void defaultProvider_usesGeminiNotNvidia() {
		when(geminiPlantVisionClient.analyzeImage(any(), any())).thenReturn("Gemini vision text");

		String result = routingPlantVisionClient.analyzeImage(new byte[] { 1 }, "image/jpeg");

		assertEquals("Gemini vision text", result);
		verify(geminiPlantVisionClient).analyzeImage(any(), eq("image/jpeg"));
		verify(nvidiaPlantVisionClient, never()).analyzeImage(any(), any());
	}

	@Test
	void nvidiaProvider_usesNvidiaNotGemini() {
		diagnosisProperties.setActiveVisionProvider(DiagnosisProperties.VISION_PROVIDER_NVIDIA);
		when(nvidiaPlantVisionClient.analyzeImage(any(), any())).thenReturn("NVIDIA vision text");

		String result = routingPlantVisionClient.analyzeImage(new byte[] { 2 }, "image/png");

		assertEquals("NVIDIA vision text", result);
		verify(nvidiaPlantVisionClient).analyzeImage(any(), eq("image/png"));
		verify(geminiPlantVisionClient, never()).analyzeImage(any(), any());
	}

	@Test
	void geminiTransientFailure_noFallbackConfigured_throwsVisionUnavailable() {
		diagnosisProperties.setVisionFallbackProvider(DiagnosisProperties.VISION_FALLBACK_NONE);
		VisionUnavailableException failure = new VisionUnavailableException(
				"gemini", VisionFailureKind.SERVICE_UNAVAILABLE, "503 Service Unavailable");
		when(geminiPlantVisionClient.analyzeImage(any(), any())).thenThrow(failure);

		VisionUnavailableException thrown = assertThrows(VisionUnavailableException.class,
				() -> routingPlantVisionClient.analyzeImage(new byte[] { 3 }, "image/jpeg"));
		assertEquals(VisionFailureKind.SERVICE_UNAVAILABLE, thrown.kind());
		verify(nvidiaPlantVisionClient, never()).analyzeImage(any(), any());
	}

	@Test
	void geminiTransientFailure_withNvidiaFallback_usesFallback() {
		when(geminiPlantVisionClient.analyzeImage(any(), any()))
				.thenThrow(new VisionUnavailableException(
						"gemini", VisionFailureKind.SERVICE_UNAVAILABLE, "503 high demand"));
		when(nvidiaPlantVisionClient.analyzeImage(any(), any())).thenReturn("NVIDIA fallback vision");

		String result = routingPlantVisionClient.analyzeImage(new byte[] { 4 }, "image/jpeg");

		assertEquals("NVIDIA fallback vision", result);
		verify(nvidiaPlantVisionClient).analyzeImage(any(), eq("image/jpeg"));
	}

	@Test
	void geminiNonTransientFailure_doesNotFallback() {
		diagnosisProperties.setVisionFallbackProvider(DiagnosisProperties.VISION_PROVIDER_NVIDIA);
		when(geminiPlantVisionClient.analyzeImage(any(), any()))
				.thenThrow(new IllegalStateException("GEMINI_API_KEY is not configured."));

		assertThrows(IllegalStateException.class,
				() -> routingPlantVisionClient.analyzeImage(new byte[] { 5 }, "image/jpeg"));
		verify(nvidiaPlantVisionClient, never()).analyzeImage(any(), any());
	}

	@Test
	void geminiTimeout_triggersNvidiaFallbackImmediately() {
		when(geminiPlantVisionClient.analyzeImage(any(), any()))
				.thenThrow(new VisionUnavailableException(
						"gemini", VisionFailureKind.TIMEOUT, "Read timed out"));
		when(nvidiaPlantVisionClient.analyzeImage(any(), any())).thenReturn("NVIDIA fallback vision");

		String result = routingPlantVisionClient.analyzeImage(new byte[] { 6 }, "image/jpeg");

		assertEquals("NVIDIA fallback vision", result);
		verify(geminiPlantVisionClient, times(1)).analyzeImage(any(), any());
		verify(nvidiaPlantVisionClient, times(1)).analyzeImage(any(), eq("image/jpeg"));
	}

	@Test
	void geminiTimeout_nvidiaAlsoFails_throwsVisionUnavailable() {
		when(geminiPlantVisionClient.analyzeImage(any(), any()))
				.thenThrow(new VisionUnavailableException(
						"gemini", VisionFailureKind.TIMEOUT, "Read timed out"));
		when(nvidiaPlantVisionClient.analyzeImage(any(), any()))
				.thenThrow(new VisionUnavailableException(
						"nvidia", VisionFailureKind.TIMEOUT, "NVIDIA vision timed out"));

		VisionUnavailableException thrown = assertThrows(VisionUnavailableException.class,
				() -> routingPlantVisionClient.analyzeImage(new byte[] { 7 }, "image/jpeg"));

		assertEquals("nvidia", thrown.provider());
		assertEquals(VisionFailureKind.TIMEOUT, thrown.kind());
		verify(nvidiaPlantVisionClient).analyzeImage(any(), eq("image/jpeg"));
	}

	@Test
	void gemini503_skipsFallbackWhenOverallDeadlineExhausted() throws Exception {
		DiagnosisCallContext.begin(new DiagnosisLatencyBudget(1, 16_000, 8_000, 8_000));
		try {
			Thread.sleep(5);
			when(geminiPlantVisionClient.analyzeImage(any(), any()))
					.thenThrow(new VisionUnavailableException(
							"gemini", VisionFailureKind.SERVICE_UNAVAILABLE, "503"));

			VisionUnavailableException thrown = assertThrows(VisionUnavailableException.class,
					() -> routingPlantVisionClient.analyzeImage(new byte[] { 8 }, "image/jpeg"));

			assertEquals("gemini", thrown.provider());
			verify(nvidiaPlantVisionClient, never()).analyzeImage(any(), any());
		} finally {
			DiagnosisCallContext.end();
		}
	}

	@Test
	void gemini503_stillFallsBackWhenOnlyVisionEnvelopeIsExhausted() throws Exception {
		DiagnosisCallContext.begin(new DiagnosisLatencyBudget(28_000, 1, 8_000, 8_000));
		try {
			Thread.sleep(5);
			when(geminiPlantVisionClient.analyzeImage(any(), any()))
					.thenThrow(new VisionUnavailableException(
							"gemini", VisionFailureKind.SERVICE_UNAVAILABLE, "503"));
			when(nvidiaPlantVisionClient.analyzeImage(any(), any())).thenReturn("NVIDIA fallback vision");

			String result = routingPlantVisionClient.analyzeImage(new byte[] { 10 }, "image/jpeg");

			assertEquals("NVIDIA fallback vision", result);
			verify(nvidiaPlantVisionClient).analyzeImage(any(), eq("image/jpeg"));
		} finally {
			DiagnosisCallContext.end();
		}
	}

	@Test
	void geminiSuccess_normalizesMissingSectionsToUnknown() {
		when(geminiPlantVisionClient.analyzeImage(any(), any()))
				.thenReturn(VisionTestFixtures.INCOMPLETE_MORPHOLOGY_ONLY);

		String result = routingPlantVisionClient.analyzeImage(new byte[] { 9 }, "image/jpeg");

		assertTrue(result.contains("TISSUE DAMAGE: UNKNOWN"));
		verify(nvidiaPlantVisionClient, never()).analyzeImage(any(), any());
	}
}
