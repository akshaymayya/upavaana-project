package com.plantdoctor.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses structured vision output into tri-state category findings.
 * Missing sections are UNKNOWN — never treated as ABSENT (negative).
 */
public record VisionObservationAssessment(
		boolean tissueSectionSeen,
		CategoryState tissueDamageState,
		boolean pestSectionSeen,
		CategoryState pestScanState,
		boolean diseaseSectionSeen,
		CategoryState diseaseSignsState,
		boolean abioticSectionSeen,
		CategoryState abioticStressState,
		boolean overallSectionSeen,
		String observationText) {

	public enum CategoryState {
		UNKNOWN,
		ABSENT,
		PRESENT
	}

	private static final Pattern PARTIAL_STRUCTURED_RESPONSE = Pattern.compile(
			"(?:#{1,6}\\s*\\d*\\.?\\s*)?(?:leaf morphology|morphology analysis|leaf shape|edge/margin|"
					+ "venation pattern|growth habit|plant identification)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern SECTION_VALUE = Pattern.compile(
			"(?:#{1,6}\\s*)?(?:\\d+[.)]\\s*)?(TISSUE DAMAGE|PEST(?:\\s+SURFACE)?\\s+SCAN|DISEASE SIGNS?|"
					+ "ABIOTIC STRESS|OVERALL(?:\\s+CONCLUSION)?)\\s*:\\s*([^\\n]+?)(?=\\.|\\n|$)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern UNKNOWN_SECTION_VALUE = Pattern.compile(
			"^(?:unknown|not\\s+assessed|n/?a|unavailable)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern NEGATIVE_SECTION_VALUE = Pattern.compile(
			"^(?:none(?:\\s+seen)?|no\\b[^,]*|negative|clear|not\\s+(?:seen|observed|detected)|"
					+ "nothing(?:\\s+seen)?|nil|absent|all\\s+(?:categories?\\s+)?(?:negative|clear)|"
					+ "appears?\\s+healthy|healthy)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern HYPOTHETICAL_SCAN_CLAUSE = Pattern.compile(
			"(?:look(?:ed)?|inspect(?:ed)?|scan(?:ned)?|check(?:ed)?|searched?)\\s+(?:the\\s+)?(?:for\\s+)?[^.;]{0,100}"
					+ "\\b(?:scale|aphid|whitefl(?:y|ies)|mealybug|mite|insect|pest|webbing)[^.;]{0,80}"
					+ "(?:;|:|,\\s*)(?:\\s*none\\s+seen|\\s*no\\s+[^.;]+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PEST_KEYWORD_BEFORE_NEGATIVE = Pattern.compile(
			"\\b(?:look(?:ed)?|inspect(?:ed)?|scan(?:ned)?|check(?:ed)?|search(?:ed)?)\\s+(?:for\\s+)?[^.;]{0,80}"
					+ "\\b(?:scale|aphid|whitefl(?:y|ies)|mealybug|mite|insect|pest|webbing)[^.;]{0,60}"
					+ "(?:;|:)\\s*(?:none\\s+seen|no\\s+(?:scale|aphids?|pests?|insects?|mites?|mealybugs?|webbing)\\b[^.;]*)",
			Pattern.CASE_INSENSITIVE);

	public boolean allCategoriesNegative() {
		return allCategoriesExplicitlyNegative();
	}

	public boolean allCategoriesExplicitlyNegative() {
		return isAssessmentComplete()
				&& tissueDamageState == CategoryState.ABSENT
				&& pestScanState == CategoryState.ABSENT
				&& diseaseSignsState == CategoryState.ABSENT
				&& abioticStressState == CategoryState.ABSENT;
	}

	public boolean isAssessmentComplete() {
		return tissueSectionSeen && pestSectionSeen && diseaseSectionSeen && abioticSectionSeen;
	}

	public boolean isIncompleteHealthAssessment() {
		return !isAssessmentComplete();
	}

	/** Legacy one-line vision with no structured health sections at all. */
	public boolean canUseFreeTextEvidence() {
		if (tissueSectionSeen || pestSectionSeen || diseaseSectionSeen || abioticSectionSeen) {
			return false;
		}
		return !PARTIAL_STRUCTURED_RESPONSE.matcher(observationText).find();
	}

	public boolean hasStructuredAssessment() {
		return tissueSectionSeen || pestSectionSeen || diseaseSectionSeen || abioticSectionSeen;
	}

	public boolean tissueDamageNegative() {
		return tissueSectionSeen && tissueDamageState == CategoryState.ABSENT;
	}

	public boolean pestScanNegative() {
		return pestSectionSeen && pestScanState == CategoryState.ABSENT;
	}

	public boolean diseaseSignsNegative() {
		return diseaseSectionSeen && diseaseSignsState == CategoryState.ABSENT;
	}

	public boolean abioticStressNegative() {
		return abioticSectionSeen && abioticStressState == CategoryState.ABSENT;
	}

	public boolean tissueDamagePositive() {
		if (tissueSectionSeen) {
			return tissueDamageState == CategoryState.PRESENT;
		}
		if (canUseFreeTextEvidence()) {
			return VisualEvidencePatterns.hasPhysicalDamageEvidence(observationText);
		}
		return false;
	}

	public List<String> missingRequiredSections() {
		List<String> missing = new ArrayList<>();
		if (!tissueSectionSeen) {
			missing.add("TISSUE DAMAGE");
		}
		if (!pestSectionSeen) {
			missing.add("PEST SURFACE SCAN");
		}
		if (!diseaseSectionSeen) {
			missing.add("DISEASE SIGNS");
		}
		if (!abioticSectionSeen) {
			missing.add("ABIOTIC STRESS");
		}
		return missing;
	}

	public String diagnosticSummary() {
		return String.format(
				"tissue=%s pest=%s disease=%s abiotic=%s overall=%s complete=%s freeText=%s",
				labelSection(tissueSectionSeen, tissueDamageState),
				labelSection(pestSectionSeen, pestScanState),
				labelSection(diseaseSectionSeen, diseaseSignsState),
				labelSection(abioticSectionSeen, abioticStressState),
				overallSectionSeen ? "seen" : "missing",
				isAssessmentComplete(),
				canUseFreeTextEvidence());
	}

	private static String labelSection(boolean seen, CategoryState state) {
		if (!seen) {
			return "UNKNOWN(missing)";
		}
		return state.name();
	}

	public static VisionObservationAssessment parse(String visionText) {
		if (visionText == null || visionText.isBlank()) {
			return empty();
		}
		boolean tissueSeen = false;
		CategoryState tissueState = CategoryState.UNKNOWN;
		boolean pestSeen = false;
		CategoryState pestState = CategoryState.UNKNOWN;
		boolean diseaseSeen = false;
		CategoryState diseaseState = CategoryState.UNKNOWN;
		boolean abioticSeen = false;
		CategoryState abioticState = CategoryState.UNKNOWN;
		boolean overallSeen = false;

		Matcher matcher = SECTION_VALUE.matcher(visionText);
		while (matcher.find()) {
			String section = matcher.group(1).toUpperCase().replaceAll("\\s+", " ");
			String value = matcher.group(2).trim();
			CategoryState state;
			if (UNKNOWN_SECTION_VALUE.matcher(value).find()) {
				state = CategoryState.UNKNOWN;
			} else {
				boolean negative = NEGATIVE_SECTION_VALUE.matcher(value).find();
				state = negative ? CategoryState.ABSENT : CategoryState.PRESENT;
			}
			if (section.startsWith("TISSUE DAMAGE")) {
				tissueSeen = true;
				tissueState = state;
			} else if (section.startsWith("PEST")) {
				pestSeen = true;
				pestState = state;
			} else if (section.startsWith("DISEASE SIGN")) {
				diseaseSeen = true;
				diseaseState = state;
			} else if (section.startsWith("ABIOTIC STRESS")) {
				abioticSeen = true;
				abioticState = state;
			} else if (section.startsWith("OVERALL")) {
				overallSeen = true;
			}
		}
		String observation = stripHypotheticalScanLanguage(visionText);
		if (tissueSeen && VisualEvidencePatterns.hasPhysicalDamageEvidence(observation)) {
			tissueState = CategoryState.PRESENT;
		}
		return new VisionObservationAssessment(
				tissueSeen, tissueState,
				pestSeen, pestState,
				diseaseSeen, diseaseState,
				abioticSeen, abioticState,
				overallSeen,
				observation);
	}

	private static VisionObservationAssessment empty() {
		return new VisionObservationAssessment(
				false, CategoryState.UNKNOWN,
				false, CategoryState.UNKNOWN,
				false, CategoryState.UNKNOWN,
				false, CategoryState.UNKNOWN,
				false, "");
	}

	private static String stripHypotheticalScanLanguage(String text) {
		String cleaned = HYPOTHETICAL_SCAN_CLAUSE.matcher(text).replaceAll(" ");
		cleaned = PEST_KEYWORD_BEFORE_NEGATIVE.matcher(cleaned).replaceAll(" ");
		return cleaned.replaceAll("\\s+", " ").trim();
	}
}
