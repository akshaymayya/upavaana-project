package com.plantdoctor.service;

import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hard rule: visible issue symptoms cannot ship as Healthy / is_healthy true.
 */
public final class DiagnosisHealthConsistency {

	private static final Logger log = LoggerFactory.getLogger(DiagnosisHealthConsistency.class);

	static final String UNIDENTIFIED = "Unidentified Issue";

	private static final Pattern NO_ISSUE_SYMPTOMS = Pattern.compile(
			"^(none|n/?a|nil|no visible( symptoms| damage| issues?)?|no (signs? of )?(disease|pests?|symptoms?|damage|issues?)( observed| visible)?|healthy( looking)?( plant)?)$",
			Pattern.CASE_INSENSITIVE);

	private DiagnosisHealthConsistency() {
	}

	public static DiagnosisResult enforce(DiagnosisResult parsed) {
		if (parsed == null) {
			return parsed;
		}
		if (!contradictsHealthy(parsed)) {
			return parsed;
		}
		log.warn(
				"Healthy contradiction coerced to Unidentified Issue: disease_name={} is_healthy={} symptoms_matched={}",
				parsed.disease_name(), parsed.is_healthy(), parsed.symptoms_matched());
		String note = parsed.confidence_note();
		String coercedNote = (note == null || note.isBlank())
				? "Low — visible symptoms present; not a healthy outcome despite missing KB match."
				: note + " Coerced from Healthy: vision listed symptoms, so this is Unidentified Issue (Low), not Healthy.";
		return new DiagnosisResult(
				parsed.plant_name(),
				UNIDENTIFIED,
				parsed.symptoms_matched(),
				parsed.solution(),
				coercedNote,
				false);
	}

	static boolean contradictsHealthy(DiagnosisResult parsed) {
		if (!symptomsDescribeIssue(parsed.symptoms_matched())) {
			return false;
		}
		boolean healthyFlag = Boolean.TRUE.equals(parsed.is_healthy());
		String disease = parsed.disease_name() == null ? "" : parsed.disease_name().trim();
		boolean healthyName = disease.equalsIgnoreCase("Healthy")
				|| disease.toLowerCase(Locale.ROOT).contains("no issues")
				|| disease.toLowerCase(Locale.ROOT).contains("no disease");
		return healthyFlag || healthyName;
	}

	static boolean symptomsDescribeIssue(String symptoms) {
		if (symptoms == null || symptoms.isBlank()) {
			return false;
		}
		String trimmed = symptoms.trim();
		if (NO_ISSUE_SYMPTOMS.matcher(trimmed).matches()) {
			return false;
		}
		return true;
	}
}
