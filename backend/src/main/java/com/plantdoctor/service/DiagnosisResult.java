package com.plantdoctor.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiagnosisResult(
		String plant_name,
		String disease_name,
		String symptoms_matched,
		String solution,
		String confidence_note,
		Boolean is_healthy
) {
}
