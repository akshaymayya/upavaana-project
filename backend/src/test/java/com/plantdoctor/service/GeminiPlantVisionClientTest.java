package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.plantdoctor.config.GeminiProperties;

class GeminiPlantVisionClientTest {

	@Test
	void extractText_readsGeminiResponseShape() {
		Map<String, Object> response = Map.of(
				"candidates", List.of(Map.of(
						"content", Map.of(
								"parts", List.of(Map.of(
										"text", "Woody shrub leaf with scale-like bumps along the midrib."))))));

		assertEquals("Woody shrub leaf with scale-like bumps along the midrib.",
				GeminiPlantVisionClient.extractText(response));
	}

	@Test
	void analyzeImage_requiresApiKey() {
		GeminiProperties properties = new GeminiProperties();
		properties.setApiKey("");
		GeminiPlantVisionClient client = new GeminiPlantVisionClient(properties);

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> client.analyzeImage(new byte[] { 1 }, "image/jpeg"));
		assertTrue(ex.getMessage().contains("GEMINI_API_KEY"));
	}

	@Test
	void extractText_throwsWhenEmpty() {
		assertThrows(RuntimeException.class, () -> GeminiPlantVisionClient.extractText(Map.of()));
	}

	@Test
	void defaultTimeoutsFailOverQuicklyToNvidia() {
		GeminiProperties properties = new GeminiProperties();
		assertEquals(4000, properties.getConnectTimeoutMs());
		assertEquals(8000, properties.getReadTimeoutMs());
		assertEquals(1, GeminiPlantVisionClient.MAX_TRANSIENT_ATTEMPTS);
	}

	@Test
	void createRestTemplate_appliesBoundedConnectAndReadTimeouts() throws Exception {
		GeminiProperties properties = new GeminiProperties();
		properties.setConnectTimeoutMs(5000);
		properties.setReadTimeoutMs(12000);

		RestTemplate restTemplate = GeminiPlantVisionClient.createRestTemplate(properties);
		assertInstanceOf(SimpleClientHttpRequestFactory.class, restTemplate.getRequestFactory());

		SimpleClientHttpRequestFactory factory =
				(SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
		assertEquals(5000, readTimeoutField(factory, "connectTimeout"));
		assertEquals(12000, readTimeoutField(factory, "readTimeout"));
	}

	private static int readTimeoutField(SimpleClientHttpRequestFactory factory, String fieldName)
			throws Exception {
		var field = SimpleClientHttpRequestFactory.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		Object value = field.get(factory);
		if (value instanceof Integer millis) {
			return millis;
		}
		if (value instanceof java.time.Duration duration) {
			return (int) duration.toMillis();
		}
		throw new AssertionError("Unexpected timeout field type: " + (value == null ? "null" : value.getClass()));
	}

	@Test
	void analyzeImage_readTimeout_failFastThenVisionUnavailable() {
		GeminiProperties properties = new GeminiProperties();
		properties.setApiKey("test-key");
		RestTemplate restTemplate = mock(RestTemplate.class);
		when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
				.thenThrow(new ResourceAccessException(
						"I/O error on POST request", new SocketTimeoutException("Read timed out")));

		GeminiPlantVisionClient client = new GeminiPlantVisionClient(properties, restTemplate);

		VisionUnavailableException ex = assertThrows(VisionUnavailableException.class,
				() -> client.analyzeImage(new byte[] { 1 }, "image/jpeg"));

		assertEquals(VisionFailureKind.TIMEOUT, ex.kind());
		assertEquals("gemini", ex.provider());
		verify(restTemplate, times(GeminiPlantVisionClient.MAX_TRANSIENT_ATTEMPTS))
				.postForObject(anyString(), any(), eq(Map.class));
	}

	@Test
	void analyzeImage_http503_failOverWithoutRetry() {
		GeminiProperties properties = new GeminiProperties();
		properties.setApiKey("test-key");
		RestTemplate restTemplate = mock(RestTemplate.class);
		when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
				.thenThrow(org.springframework.web.client.HttpServerErrorException.create(
						org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
						"Service Unavailable", null, null, null));

		GeminiPlantVisionClient client = new GeminiPlantVisionClient(properties, restTemplate);

		VisionUnavailableException ex = assertThrows(VisionUnavailableException.class,
				() -> client.analyzeImage(new byte[] { 1 }, "image/jpeg"));

		assertEquals(VisionFailureKind.SERVICE_UNAVAILABLE, ex.kind());
		verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(Map.class));
	}

	@Test
	void analyzeImage_incompleteVision_doesNotMakeSecondModelCall() {
		GeminiProperties properties = new GeminiProperties();
		properties.setApiKey("test-key");
		RestTemplate restTemplate = mock(RestTemplate.class);
		when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
				.thenReturn(Map.of(
						"candidates", List.of(Map.of(
								"content", Map.of(
										"parts", List.of(Map.of(
												"text", VisionTestFixtures.INCOMPLETE_MORPHOLOGY_ONLY)))))));

		GeminiPlantVisionClient client = new GeminiPlantVisionClient(properties, restTemplate);
		String text = client.analyzeImage(new byte[] { 1 }, "image/jpeg");

		assertEquals(VisionTestFixtures.INCOMPLETE_MORPHOLOGY_ONLY, text);
		verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(Map.class));
	}
}
