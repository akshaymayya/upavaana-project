package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

class NvidiaClientServiceVisionPromptTest {

	@Test
	void visionAnalysisPrompt_requiresIndependentPestSurfaceScan() {
		String prompt = VisionAnalysisPrompts.TEXT;

		assertTrue(prompt.toLowerCase().contains("pest surface scan")
				|| prompt.toLowerCase().contains("independent assessment"));
		assertTrue(prompt.toLowerCase().contains("scale") || prompt.toLowerCase().contains("attached"));
		assertTrue(prompt.toLowerCase().contains("absence of discoloration")
				|| prompt.toLowerCase().contains("does not mean"));
		assertTrue(prompt.toLowerCase().contains("midrib") || prompt.toLowerCase().contains("vein"));
		assertTrue(prompt.toLowerCase().contains("healthy"));
		assertEquals(VisionAnalysisPrompts.TEXT, NvidiaClientService.VISION_ANALYSIS_PROMPT);
	}

	@Test
	void nvidiaVisionPrompt_isCompactLabeledContract_notFullGeminiEssay() {
		String nvidia = VisionAnalysisPrompts.NVIDIA;
		assertTrue(nvidia.contains("TISSUE DAMAGE:"));
		assertTrue(nvidia.contains("PEST SURFACE SCAN:"));
		assertTrue(nvidia.contains("DISEASE SIGNS:"));
		assertTrue(nvidia.contains("ABIOTIC STRESS:"));
		assertTrue(nvidia.contains("OVERALL CONCLUSION:"));
		assertTrue(nvidia.toLowerCase().contains("variegation"));
		assertFalse(nvidia.contains("lanceolate"));
		assertTrue(nvidia.length() < VisionAnalysisPrompts.TEXT.length() / 2);
	}

	@Test
	void nvidiaVisionRequest_usesCompactPromptAndHtmlImgNotOpenAiImageUrlObject() {
		String bodyContent = NvidiaClientService.nvidiaVisionUserContent(
				VisionAnalysisPrompts.NVIDIA, "data:image/jpeg;base64,abc");
		assertTrue(bodyContent.contains("<img src=\"data:image/jpeg;base64,abc\" />"));
		assertTrue(bodyContent.contains("TISSUE DAMAGE"));
		assertFalse(bodyContent.contains("lanceolate"));
		Map<String, Object> request = NvidiaClientService.nvidiaVisionRequestBody(
				"meta/llama-3.2-11b-vision-instruct", VisionAnalysisPrompts.NVIDIA, "data:image/jpeg;base64,abc");
		assertEquals(NvidiaClientService.NVIDIA_VISION_MAX_TOKENS, request.get("max_tokens"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
		assertTrue(messages.get(0).get("content") instanceof String);
		assertFalse(messages.get(0).get("content") instanceof List);
	}

	@Test
	void estimateInputTokens_matchesObserved328kDataUriTokenization() {
		String fake = "x".repeat(1_314_700);
		assertEquals(328_675, NvidiaClientService.estimateInputTokens(fake));
	}

	@Test
	void prepareNvidiaVisionPayload_downscalesUntilUnderTokenCap() throws Exception {
		java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(1600, 1600, java.awt.image.BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = image.createGraphics();
		g.setColor(java.awt.Color.GREEN);
		g.fillRect(0, 0, 1600, 1600);
		g.dispose();
		java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
		javax.imageio.ImageIO.write(image, "png", raw);
		byte[] original = raw.toByteArray();
		NvidiaClientService.NvidiaVisionPayload payload =
				NvidiaClientService.prepareNvidiaVisionPayload(original, "image/png");
		assertTrue(payload.estimatedInputTokens() <= NvidiaClientService.NVIDIA_VISION_TARGET_INPUT_TOKENS);
		assertTrue(payload.estimatedInputTokens() + NvidiaClientService.NVIDIA_VISION_MAX_TOKENS
				< NvidiaClientService.NVIDIA_VISION_CONTEXT_LIMIT_TOKENS);
		assertTrue(payload.userContent().startsWith(VisionAnalysisPrompts.NVIDIA));
		assertFalse(payload.userContent().contains("lanceolate"));
	}

	@Test
	void prepareNvidiaVisionPayload_rejectsUndecodablePayloadAboveCap() {
		byte[] huge = new byte[250_000];
		VisionUnavailableException thrown = org.junit.jupiter.api.Assertions.assertThrows(
				VisionUnavailableException.class,
				() -> NvidiaClientService.prepareNvidiaVisionPayload(huge, "image/jpeg"));
		assertTrue(thrown.getMessage().contains("estimatedInputTokens"));
	}

	@Test
	void prepareNvidiaVisionPayload_compressesNoisyPhotoToLatencyTarget() {
		java.awt.image.BufferedImage image =
				new java.awt.image.BufferedImage(900, 900, java.awt.image.BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 900; y++) {
			for (int x = 0; x < 900; x++) {
				image.setRGB(x, y, (x * 17 + y * 31) << 8);
			}
		}
		byte[] jpeg = NvidiaClientService.jpegDownscale(toPng(image), 900, 0.9f);
		org.junit.jupiter.api.Assertions.assertNotNull(jpeg);
		String dataUri = "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(jpeg);
		int rawTokens = NvidiaClientService.estimateInputTokens(
				NvidiaClientService.nvidiaVisionUserContent(VisionAnalysisPrompts.NVIDIA, dataUri));
		assertTrue(rawTokens > NvidiaClientService.NVIDIA_VISION_TARGET_INPUT_TOKENS);
		NvidiaClientService.NvidiaVisionPayload prepared =
				NvidiaClientService.prepareNvidiaVisionPayload(jpeg, "image/jpeg");
		assertTrue(prepared.estimatedInputTokens() <= NvidiaClientService.NVIDIA_VISION_TARGET_INPUT_TOKENS);
		assertTrue(prepared.sentImageBytes() <= jpeg.length);
	}

	@Test
	void nvidiaHttpClient_appliesConnectAndReadTimeouts() throws Exception {
		RestTemplate restTemplate = NvidiaClientService.httpClient(4000, 8000);
		assertTrue(restTemplate.getRequestFactory() instanceof SimpleClientHttpRequestFactory);
		SimpleClientHttpRequestFactory factory =
				(SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
		assertEquals(4000, readTimeoutField(factory, "connectTimeout"));
		assertEquals(8000, readTimeoutField(factory, "readTimeout"));
	}

	private static byte[] toPng(java.awt.image.BufferedImage image) {
		try {
			java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
			javax.imageio.ImageIO.write(image, "png", raw);
			return raw.toByteArray();
		} catch (Exception ex) {
			throw new AssertionError(ex);
		}
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
}
