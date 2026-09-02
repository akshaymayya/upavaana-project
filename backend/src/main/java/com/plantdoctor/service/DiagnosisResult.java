package com.plantdoctor.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiagnosisResult(
		String plant_name,
		String disease_name,
		@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
		List<String> symptoms_matched,
		String solution,
		String confidence_note,
		Boolean is_healthy
) {
	public DiagnosisResult {
		symptoms_matched = copySymptoms(symptoms_matched);
	}

	/** Convenience for tests and pipeline string literals. JSON still serializes an array. */
	public DiagnosisResult(
			String plant_name,
			String disease_name,
			String symptoms_matched,
			String solution,
			String confidence_note,
			Boolean is_healthy) {
		this(plant_name, disease_name, stringAsList(symptoms_matched), solution, confidence_note, is_healthy);
	}

	@JsonIgnore
	public String symptomsEvidenceText() {
		if (symptoms_matched == null || symptoms_matched.isEmpty()) {
			return "";
		}
		return symptoms_matched.stream().filter(Objects::nonNull).collect(Collectors.joining("\n"));
	}

	@JsonIgnore
	public boolean hasSymptomsMatched() {
		return symptoms_matched != null && symptoms_matched.stream().anyMatch(s -> s != null && !s.isBlank());
	}

	private static List<String> copySymptoms(List<String> in) {
		if (in == null || in.isEmpty()) {
			return List.of();
		}
		return List.copyOf(in.stream().filter(s -> s != null && !s.isBlank()).toList());
	}

	private static List<String> stringAsList(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return List.of(value.trim());
	}
}
