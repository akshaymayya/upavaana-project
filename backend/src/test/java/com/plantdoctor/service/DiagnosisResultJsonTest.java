package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

class DiagnosisResultJsonTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void deserializesSymptomsMatchedAsListOfStrings() throws Exception {
		String json = """
				{"plant_name":"Pothos","disease_name":"Leaf-spot disease",
				"symptoms_matched":["yellow spots","brown lesions"],
				"solution":"Remove affected leaves","confidence_note":"High — lesions visible","is_healthy":false}
				""";
		DiagnosisResult parsed = mapper.readValue(json, DiagnosisResult.class);
		assertEquals(List.of("yellow spots", "brown lesions"), parsed.symptoms_matched());
		assertEquals("yellow spots\nbrown lesions", parsed.symptomsEvidenceText());
		assertEquals("Pothos", parsed.plant_name());
		assertEquals("Leaf-spot disease", parsed.disease_name());
		assertEquals("Remove affected leaves", parsed.solution());
		assertEquals(Boolean.FALSE, parsed.is_healthy());
	}

	@Test
	void deserializesLegacySymptomsMatchedStringAsSingletonList() throws Exception {
		String json = """
				{"plant_name":"Pothos","disease_name":"Leaf-spot disease",
				"symptoms_matched":"yellow spots",
				"solution":"Remove affected leaves","confidence_note":"High","is_healthy":false}
				""";
		DiagnosisResult parsed = mapper.readValue(json, DiagnosisResult.class);
		assertEquals(List.of("yellow spots"), parsed.symptoms_matched());
	}

	@Test
	void rejectsArrayForStringFields() {
		String json = """
				{"plant_name":["Pothos"],"disease_name":"Leaf-spot disease",
				"symptoms_matched":["yellow spots"],
				"solution":"Remove affected leaves","confidence_note":"High","is_healthy":false}
				""";
		assertThrows(MismatchedInputException.class, () -> mapper.readValue(json, DiagnosisResult.class));
	}

	@Test
	void serializesSymptomsMatchedAsJsonArray() throws Exception {
		DiagnosisResult result = new DiagnosisResult(
				"Pothos", "Leaf-spot disease", List.of("yellow spots", "brown lesions"),
				"Remove affected leaves", "High", false);
		String json = mapper.writeValueAsString(result);
		assertTrue(json.contains("\"symptoms_matched\":["));
		assertFalse(json.contains("\"symptoms_matched\":\"yellow spots, brown lesions\""));
	}
}
