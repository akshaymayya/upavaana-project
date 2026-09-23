package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NvidiaClientServiceSynthesisPromptTest {

	@Test
	void synthesisPrompt_commitsOneDiagnosisAndFormatsSolution() {
		String prompt = NvidiaClientService.SYNTHESIS_SYSTEM_PROMPT;
		assertTrue(prompt.contains("One disease_name only") || prompt.contains("COMMIT"));
		assertTrue(prompt.toLowerCase().contains("holes"));
		assertTrue(prompt.toLowerCase().contains("chew"));
		assertTrue(prompt.toLowerCase().contains("yellowing") || prompt.toLowerCase().contains("wilting"));
		assertTrue(prompt.contains("**neem oil spray**") || prompt.contains("double asterisks"));
		assertTrue(prompt.contains("\\n") || prompt.toLowerCase().contains("newline"));
		assertFalse(prompt.contains("4 to 6 short numbered steps"));
	}

	@Test
	void evidenceRules_preferSpecificOverGeneric() {
		String rules = NvidiaClientService.SYNTHESIS_EVIDENCE_AND_FORMAT;
		assertTrue(rules.toLowerCase().contains("outranks") || rules.toLowerCase().contains("outweigh"));
		assertTrue(rules.contains("confidence_note"));
		assertTrue(rules.contains("**neem oil spray**"));
	}
}
