package com.plantdoctor.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.plantdoctor.config.DiagnosisProperties;
import com.plantdoctor.config.GeminiProperties;

/**
 * Gemini vision analysis for the live MVP diagnosis pipeline.
 */
@Service
public class GeminiPlantVisionClient implements PlantVisionClient {

	private static final Logger log = LoggerFactory.getLogger(GeminiPlantVisionClient.class);
	private static final String PROVIDER_NAME = DiagnosisProperties.VISION_PROVIDER_GEMINI;
	static final int MAX_TRANSIENT_ATTEMPTS = 1;

	private final GeminiProperties geminiProperties;
	private final RestTemplate restTemplate;

	@Autowired
	public GeminiPlantVisionClient(GeminiProperties geminiProperties) {
		this(geminiProperties, createRestTemplate(geminiProperties));
	}

	GeminiPlantVisionClient(GeminiProperties geminiProperties, RestTemplate restTemplate) {
		this.geminiProperties = geminiProperties;
		this.restTemplate = restTemplate;
		log.info("Gemini HTTP timeouts configured: connect={}ms read={}ms maxAttempts={}",
				geminiProperties.getConnectTimeoutMs(),
				geminiProperties.getReadTimeoutMs(),
				MAX_TRANSIENT_ATTEMPTS);
	}

	static RestTemplate createRestTemplate(GeminiProperties geminiProperties) {
		return createRestTemplate(geminiProperties.getConnectTimeoutMs(), geminiProperties.getReadTimeoutMs());
	}

	static RestTemplate createRestTemplate(int connectTimeoutMs, int readTimeoutMs) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeoutMs);
		factory.setReadTimeout(readTimeoutMs);
		return new RestTemplate(factory);
	}

	@Override
	public String analyzeImage(byte[] imageBytes, String mimeType) {
		if (!StringUtils.hasText(geminiProperties.getApiKey())) {
			throw new IllegalStateException("GEMINI_API_KEY is not configured.");
		}

		String base64Image = Base64.getEncoder().encodeToString(imageBytes);
		String effectiveMime = StringUtils.hasText(mimeType) ? mimeType : "image/jpeg";

		String url = geminiProperties.getBaseUrl().replaceAll("/$", "")
				+ "/models/" + geminiProperties.getModel() + ":generateContent?key="
				+ geminiProperties.getApiKey();

		Exception lastException = null;
		long overallStart = System.currentTimeMillis();
		for (int attempt = 1; attempt <= MAX_TRANSIENT_ATTEMPTS; attempt++) {
			try {
				String text = requestVisionText(url, base64Image, effectiveMime, VisionAnalysisPrompts.TEXT, attempt);
				VisionObservationAssessment assessment = VisionObservationAssessment.parse(text);
				log.info("Gemini vision section coverage: {}", assessment.diagnosticSummary());
				if (!assessment.isAssessmentComplete()) {
					log.warn("Gemini vision response incomplete — missing: {}. Returning first response (no second model call)",
							assessment.missingRequiredSections());
				}
				return text;
			} catch (Exception ex) {
				lastException = ex;
				VisionFailureKind kind = VisionFailureSupport.classify(ex);
				long elapsed = System.currentTimeMillis() - overallStart;
				log.warn(
						"Gemini vision failure attempt={}/{} elapsed={}ms connectTimeout={}ms readTimeout={}ms kind={}: {}",
						attempt,
						MAX_TRANSIENT_ATTEMPTS,
						elapsed,
						geminiProperties.getConnectTimeoutMs(),
						geminiProperties.getReadTimeoutMs(),
						kind == null ? "NON_TRANSIENT" : kind,
						ex.getMessage());
				VisionUnavailableException unavailable = VisionFailureSupport.toUnavailable(PROVIDER_NAME, ex);
				boolean retry = unavailable != null
						&& attempt < MAX_TRANSIENT_ATTEMPTS
						&& VisionFailureSupport.shouldRetrySameProvider(
								unavailable.kind(),
								DiagnosisCallContext.current().remainingTotalMs());
				if (retry) {
					long backoff = VisionFailureSupport.backoffMillis(attempt);
					log.warn("Gemini transient vision failure — retrying in {} ms", backoff);
					sleepQuietly(backoff);
					continue;
				}
				if (unavailable != null) {
					log.warn("Gemini vision exhausted retries after {} ms — throwing VisionUnavailableException ({})",
							elapsed, unavailable.kind());
					throw unavailable;
				}
				throw new VisionUnavailableException(
						PROVIDER_NAME,
						VisionFailureKind.UNKNOWN,
						"Error analyzing image via Gemini: " + ex.getMessage(),
						ex);
			}
		}

		VisionUnavailableException unavailable = VisionFailureSupport.toUnavailable(PROVIDER_NAME, lastException);
		if (unavailable != null) {
			throw unavailable;
		}
		throw new VisionUnavailableException(
				PROVIDER_NAME,
				VisionFailureKind.UNKNOWN,
				"Error analyzing image via Gemini (failed after " + MAX_TRANSIENT_ATTEMPTS + " attempts): "
						+ (lastException == null ? "unknown" : lastException.getMessage()),
				lastException);
	}

	private String requestVisionText(
			String url,
			String base64Image,
			String mimeType,
			String prompt,
			int attempt) {
		log.info("Calling Gemini vision model (attempt {}/{}): {} connectTimeout={}ms readTimeout={}ms",
				attempt,
				MAX_TRANSIENT_ATTEMPTS,
				geminiProperties.getModel(),
				geminiProperties.getConnectTimeoutMs(),
				geminiProperties.getReadTimeoutMs());
		long callStart = System.currentTimeMillis();
		Map<String, Object> requestBody = buildRequestBody(base64Image, mimeType, prompt);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
		Map<?, ?> response = geminiHttp().postForObject(url, entity, Map.class);
		long callDuration = System.currentTimeMillis() - callStart;
		log.info("Gemini vision call succeeded in {} ms (attempt {})", callDuration, attempt);
		return extractText(response);
	}

	private RestTemplate geminiHttp() {
		if (!DiagnosisCallContext.isActive()) {
			return restTemplate;
		}
		DiagnosisLatencyBudget budget = DiagnosisCallContext.current();
		int remaining = budget.remainingTotalMs();
		if (!budget.canStartProviderCall()) {
			throw new VisionUnavailableException(
					PROVIDER_NAME,
					VisionFailureKind.TIMEOUT,
					"Gemini vision skipped — overall diagnosis deadline exhausted");
		}
		int connect = budget.capTimeout(geminiProperties.getConnectTimeoutMs(), remaining);
		int read = budget.capTimeout(geminiProperties.getReadTimeoutMs(), remaining);
		if (connect == geminiProperties.getConnectTimeoutMs() && read == geminiProperties.getReadTimeoutMs()) {
			return restTemplate;
		}
		log.info("Gemini per-call timeouts connect={}ms read={}ms remainingTotalMs={}", connect, read, remaining);
		return createRestTemplate(connect, read);
	}

	private static void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Retry interrupted: " + ie.getMessage(), ie);
		}
	}

	private static Map<String, Object> buildRequestBody(String base64Image, String mimeType, String promptText) {
		Map<String, Object> textPart = Map.of("text", promptText);
		Map<String, Object> imagePart = Map.of(
				"inline_data", Map.of(
						"mime_type", mimeType,
						"data", base64Image));

		return Map.of(
				"contents", List.of(Map.of(
						"role", "user",
						"parts", List.of(textPart, imagePart))),
				"generationConfig", Map.of("maxOutputTokens", 2048));
	}

	@SuppressWarnings("unchecked")
	static String extractText(Map<?, ?> response) {
		if (response == null) {
			throw new RuntimeException("Received empty response from Gemini vision API.");
		}

		List<?> candidates = (List<?>) response.get("candidates");
		if (candidates == null || candidates.isEmpty()) {
			throw new RuntimeException("No candidates returned from Gemini vision API.");
		}

		Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
		Map<?, ?> content = (Map<?, ?>) candidate.get("content");
		if (content == null) {
			throw new RuntimeException("Gemini vision response missing content.");
		}

		List<?> parts = (List<?>) content.get("parts");
		if (parts == null || parts.isEmpty()) {
			throw new RuntimeException("Gemini vision response missing content parts.");
		}

		Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
		Object text = firstPart.get("text");
		if (text == null || text.toString().isBlank()) {
			throw new RuntimeException("Gemini vision response contained no text.");
		}
		return text.toString();
	}
}
