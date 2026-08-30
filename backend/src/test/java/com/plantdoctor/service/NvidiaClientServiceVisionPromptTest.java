package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NvidiaClientServiceVisionPromptTest {

	@Test
	void visionAnalysisPrompt_instructsScanForSmallPestDamageAndPerLeafExam() {
		String prompt = NvidiaClientService.VISION_ANALYSIS_PROMPT;

		assertTrue(prompt.toLowerCase().contains("small holes") || prompt.toLowerCase().contains("chew marks"));
		assertTrue(prompt.toLowerCase().contains("each visible leaf") || prompt.toLowerCase().contains("each leaf"));
		assertTrue(prompt.toLowerCase().contains("healthy"));
		assertTrue(prompt.toLowerCase().contains("entire") || prompt.toLowerCase().contains("whole"));
	}
}
