package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.plantdoctor.entity.Disease;
import com.plantdoctor.entity.Plant;
import com.plantdoctor.service.DiseaseCandidate.MatchType;

class NvidiaClientServiceSynthesisPromptTest {

	@Test
	void synthesisPrompt_commitsOneDiagnosisAndFormatsSolution() {
		String prompt = NvidiaClientService.SYNTHESIS_SYSTEM_PROMPT;
		assertTrue(prompt.contains("One disease_name only") || prompt.contains("COMMIT"));
		assertTrue(prompt.toLowerCase().contains("pest evidence") || prompt.toLowerCase().contains("attached insects"));
		assertTrue(prompt.toLowerCase().contains("independently") || prompt.toLowerCase().contains("absence of one"));
		assertTrue(prompt.toLowerCase().contains("leaf-spot") || prompt.toLowerCase().contains("lesion"));
		assertTrue(prompt.contains("**remove affected leaves**") || prompt.contains("double asterisks"));
		assertTrue(prompt.contains("\\n") || prompt.toLowerCase().contains("newline"));
		assertFalse(prompt.contains("4 to 6 short numbered steps"));
		assertFalse(prompt.toLowerCase().contains("pick the pest/chew diagnosis"));
	}

	@Test
	void evidenceRules_requireCauseTreatmentMatch() {
		String rules = NvidiaClientService.SYNTHESIS_EVIDENCE_AND_FORMAT;
		assertTrue(rules.toLowerCase().contains("cause match") || rules.toLowerCase().contains("treatment must match"));
		assertTrue(rules.contains("confidence_note"));
		assertTrue(rules.toLowerCase().contains("unidentified issue"));
		assertTrue(rules.toLowerCase().contains("do not recommend pest control"));
	}

	@Test
	void groqCompactPrompt_omitsKbSolutionDumpAndKeepsSafetyRules() {
		Plant plant = new Plant("Money Plant");
		Disease disease = new Disease(plant, "Root Rot", "long description ".repeat(40),
				"Yellowing leaves, mushy roots, foul odor from soil",
				"Overwatering and poor drainage",
				"Reduce watering and repot into fresh mix following a long schedule");
		String prompt = NvidiaClientService.compactSynthesisUserPrompt(
				"TISSUE DAMAGE: PRESENT holes. PEST SURFACE SCAN: ABSENT. DISEASE SIGNS: UNKNOWN. ABIOTIC STRESS: ABSENT.",
				List.of(new DiseaseCandidate(disease, MatchType.PLANT_NAME)));

		assertTrue(prompt.contains("STRUCTURED VISION"));
		assertTrue(prompt.contains("Root Rot"));
		assertTrue(prompt.contains("Yellowing leaves"));
		assertFalse(prompt.toLowerCase().contains("long description"));
		assertFalse(prompt.contains("Reduce watering"));
		assertTrue(NvidiaClientService.GROQ_SYNTHESIS_SYSTEM_PROMPT.contains("array of strings"));
		assertTrue(NvidiaClientService.GROQ_SYNTHESIS_SYSTEM_PROMPT.contains("[\"yellow spots\""));
		assertTrue(NvidiaClientService.GROQ_SYNTHESIS_SYSTEM_PROMPT.length()
				< NvidiaClientService.SYNTHESIS_SYSTEM_PROMPT.length());
		assertTrue(prompt.contains("TISSUE DAMAGE") || prompt.contains("coverage:"));
		assertFalse(prompt.contains("First describe LEAF MORPHOLOGY"));
	}

	@Test
	void compactVisionEvidence_keepsLabeledSectionsNotFullDump() {
		String vision = "Pothos with green leaves.\n"
				+ "TISSUE DAMAGE: none seen after scanning\n"
				+ "PEST SURFACE SCAN: none seen after scanning\n"
				+ "DISEASE SIGNS: none seen\n"
				+ "ABIOTIC STRESS: none seen\n"
				+ "OVERALL CONCLUSION: Healthy\n"
				+ "Long morphology dump ".repeat(80);
		String compact = NvidiaClientService.compactVisionEvidence(vision);
		assertTrue(compact.contains("TISSUE DAMAGE:"));
		assertTrue(compact.contains("PEST SURFACE SCAN:"));
		assertTrue(compact.length() < vision.length());
	}
}
