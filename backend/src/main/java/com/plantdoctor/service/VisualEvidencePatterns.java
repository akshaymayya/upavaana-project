package com.plantdoctor.service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Shared detection of pest, disease, and abnormality language in vision or synthesis text.
 * Strips negated clauses before matching so "no insects seen" is not counted as pest evidence.
 */
public final class VisualEvidencePatterns {

	private static final Pattern NEGATED_CLAUSE = Pattern.compile(
			"\\b(?:no|without|none|absent)\\s+[^.;]+|"
					+ "\\bnot\\s+(?:any\\s+)?(?:insects?|pests?|webbing|mites?|aphids?|chewing|chew(?:ing)? marks?|scale)\\b[^.;]*",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PEST_EVIDENCE = Pattern.compile(
			"\\b(scale(?:[- ]like)?(?:\\s+insects?)?|scale insects?|mealybugs?|aphids?|whitefl(?:y|ies)|"
					+ "spider mites?|thrips|mites?|caterpillars?|larvae?|grubs?|"
					+ "(?:caterpillars?|larvae?|grubs?|insects?) (?:visible|present|attached)|"
					+ "visible (?:caterpillars?|larvae?|grubs?|insects?)|"
					+ "insects? (?:on|attached|present)|"
					+ "attached (?:to|on)|colonies?|clusters? of (?:small )?(?:bumps|insects?|organisms?)|"
					+ "eggs? (?:on|attached)|webbing|stippling|leaf miners?|frass|sucking pests?|"
					+ "waxy (?:bumps|coatings?)|shell[- ]like (?:bumps|discs?)|armored (?:scale|insects?)|"
					+ "stationary (?:bumps|insects?|structures?)|oval (?:bumps|structures?|organisms?)|"
					+ "bumps (?:on|along)|discs? (?:on|along)|infestation|pest (?:present|visible|evidence)|"
					+ "along (?:the )?(?:midrib|central vein|veins?|leaf surface)|visible insects?)\\b",
			Pattern.CASE_INSENSITIVE);

	/** Physical tissue loss / chewing damage — abnormality even when no insect is currently visible. */
	private static final Pattern PHYSICAL_DAMAGE_EVIDENCE = Pattern.compile(
			"\\b(holes?|perforations?|missing (?:leaf )?(?:tissue|sections?|portions?|areas?|parts?)|"
					+ "torn|tears?|notched (?:leaves?|margins?)|"
					+ "ragged (?:edges?|margins?|holes?)|chew(?:ing|ed)?(?:\\s+(?:damage|marks?|holes?))?|"
					+ "feeding holes?|skeletoni[sz]ed|irregular (?:margins?|edges?|holes?)|"
					+ "mechanical damage|leaf blade damage|punched[- ]out|lacy (?:damage|foliage)|"
					+ "multiple holes|large holes|shot[- ]holes|hole(?:s)? through)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern DISEASE_EVIDENCE = Pattern.compile(
			"\\b(lesions?|necrotic|necrosis|halos?|blight|concentric|leaf[- ]spots?|mildew|powdery|"
					+ "bacterial|fungal (?:spots?|growth)|mold|rot(?:ting)?|yellowing|discoloration|"
					+ "dark (?:brown |black )?(?:spots?|patches?)|brown spots?|black spots?|"
					+ "tan centers?|pale centers?|lighter centers?|damaged (?:tissue|leaves)|"
					+ "affected leaves|wilt(?:ing|ed)?|deform(?:ed|ation)|extensive (?:damage|yellowing))\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern ABIOTIC_EVIDENCE = Pattern.compile(
			"\\b(scorch|bleach(?:ed|ing)?|crisp brown|wilting|drought stress|sun damage|heat stress)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern UNCERTAIN_PEST = Pattern.compile(
			"\\b(possible|probably|might be|could be|uncertain|unclear if|may be|resembles|consistent with a possible|"
					+ "suggestive of)\\b[^.;]{0,80}\\b(scale|pest|insect|mite|mealybug|aphid|infestation)\\b|"
					+ "\\b(scale|pest|insect|mite|mealybug|aphid|bumps?)\\b[^.;]{0,80}\\b(possible|might be|could be|uncertain)\\b",
			Pattern.CASE_INSENSITIVE);

	static final Pattern CLAIMS_NO_PROBLEMS = Pattern.compile(
			"none visible|no visible symptoms|appear healthy|looks? healthy|"
					+ "no (?:holes|discoloration|pests?|pest activity|lesions?|leaf[- ]?spots?|damage|issues?|necrosis|abnormalit|disease signs?|waterlogging)|"
					+ "not detected|leaves? (?:look|appear) (?:fine|healthy|normal)|without (?:damage|symptoms|issues)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern EXPLICIT_NO_ABNORMALITY = Pattern.compile(
			"\\bno\\s+(?:visible\\s+)?(?:leaf[- ]?)?spots?\\b|"
					+ "\\bno\\s+(?:visible\\s+)?(?:lesions?|necrosis|necrotic(?:\\s+tissue)?)\\b|"
					+ "\\bno\\s+(?:visible\\s+)?(?:pests?|pest activity|insects?|webbing)\\b|"
					+ "\\bno\\s+(?:meaningful\\s+)?abnormalit(?:y|ies)\\b|"
					+ "\\b(?:none|no)\\s+visible\\s*[-—]\\s*no\\s+[^.;]+\\b|"
					+ "\\bno\\s+[^.;]{0,100}\\b(?:spots?|lesions?|necrosis|pests?|discoloration|damage|disease signs?)\\b|"
					+ "\\b(?:all\\s+)?categories?\\s+(?:1[-–]4\\s+)?(?:negative|clear)\\b|"
					+ "\\bappears?\\s+healthy\\b|"
					+ "\\bhealthy[- ]looking\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern NORMAL_VARIATION = Pattern.compile(
			"\\b(variegat(?:ed|ion)|normal\\s+(?:yellow|green|cream|coloration|pigmentation|pattern)|"
					+ "natural\\s+(?:variegation|coloration|pigmentation|yellow|green|cream)|cultivar|marbling|"
					+ "chartreuse|cream[- ]colou?red|golden[- ](?:yellow|green)|yellow[-/]\\s*green\\s+(?:variegation|pattern|coloration))\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern DEFINITE_DISEASE_SIGNS = Pattern.compile(
			"\\b(lesions?|necrotic|necrosis|halos?|blight|concentric|leaf[- ]spots?|mildew|powdery|"
					+ "bacterial spots?|fungal (?:spots?|growth)|mold|rot(?:ting)?|"
					+ "dark (?:brown |black )?(?:spots?|patches?)|brown spots?|black spots?|"
					+ "tan centers?|pale centers?|lighter centers?|damaged (?:tissue|leaves)|"
					+ "affected leaves|wilt(?:ing|ed)?|deform(?:ed|ation)|extensive damage)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PATHOLOGICAL_COLORATION = Pattern.compile(
			"\\b(chlorotic|interveinal yellowing|uniform yellowing|yellowing (?:leaves|foliage|of)|spreading yellow|"
					+ "pathological discoloration|abnormal discoloration)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern AMBIGUOUS_ABNORMALITY = Pattern.compile(
			"\\b(unusual\\s+discoloration|unclear\\s+(?:cause|symptoms?)|uncertain|possible\\s+(?:issue|problem|disease)|"
					+ "might be|could be|subtle\\s+(?:discoloration|yellowing|spots?)|mild\\s+(?:discoloration|yellowing)|"
					+ "some\\s+discoloration|cause unclear)\\b",
			Pattern.CASE_INSENSITIVE);

	static final Pattern HEALTHY_CARE = Pattern.compile(
			"continue (?:your )?(?:current )?care|maintain (?:current )?care|routine care|keep (?:up )?current care",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern INSUFFICIENT_IMAGE = Pattern.compile(
			"\\b(too blurry|blurry|cannot assess|can't assess|unable to (?:assess|determine|identify)|"
					+ "insufficient (?:image|detail|evidence|quality)|unreliable (?:read|assessment|symptom read)|"
					+ "no reliable (?:read|assessment|symptom read)|"
					+ "poor (?:image )?quality|not enough (?:detail|information)|image too (?:dark|overexposed))\\b",
			Pattern.CASE_INSENSITIVE);

	private VisualEvidencePatterns() {
	}

	public static String stripNegatedClauses(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		return NEGATED_CLAUSE.matcher(text).replaceAll(" ");
	}

	public static boolean hasPestEvidence(String text) {
		return PEST_EVIDENCE.matcher(stripNegatedClauses(text)).find();
	}

	public static boolean hasPhysicalDamageEvidence(String text) {
		return PHYSICAL_DAMAGE_EVIDENCE.matcher(stripNegatedClauses(text)).find();
	}

	/**
	 * Visible holes, missing tissue, or chewing — abnormality even when TISSUE DAMAGE was labeled ABSENT
	 * (models often write "none seen" then describe holes in morphology).
	 */
	public static boolean hasCrediblePhysicalDamageEvidence(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(text);
		if (assessment.tissueDamagePositive()) {
			return true;
		}
		return hasPhysicalDamageEvidence(assessment.observationText());
	}

	/** Pest evidence from observed vision only; respects structured PEST SURFACE SCAN negatives. */
	public static boolean hasCrediblePestEvidence(String text) {
		return hasCrediblePestEvidence(text, "");
	}

	/**
	 * Pest evidence from vision observations. Synthesis symptoms may supplement only when vision
	 * does not contain a structured pest-scan negative and vision alone is inconclusive.
	 */
	public static boolean hasCrediblePestEvidence(String visionText, String synthesisSymptoms) {
		if (visionText == null || visionText.isBlank()) {
			return hasPestEvidence(stripNegatedClauses(synthesisSymptoms));
		}
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(visionText);
		if (assessment.pestSectionSeen()
				&& assessment.pestScanState() == VisionObservationAssessment.CategoryState.ABSENT) {
			return false;
		}
		return hasPestEvidence(stripNegatedClauses(assessment.observationText()));
	}

	public static boolean hasDiseaseEvidence(String text) {
		return DISEASE_EVIDENCE.matcher(stripNegatedClauses(text)).find();
	}

	/** Disease evidence that excludes normal variegation/coloration and respects explicit negative findings. */
	public static boolean hasCredibleDiseaseEvidence(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(text);
		if (assessment.diseaseSectionSeen()) {
			if (assessment.diseaseSignsState() == VisionObservationAssessment.CategoryState.ABSENT) {
				return false;
			}
		} else if (!assessment.canUseFreeTextEvidence()) {
			return false;
		}
		String observation = assessment.observationText();
		if (hasExplicitNoAbnormality(observation) && !hasPositiveAbnormalityAfterNegation(observation)) {
			return false;
		}
		String scrubbed = stripNegatedClauses(observation);
		if (!DISEASE_EVIDENCE.matcher(scrubbed).find()) {
			return false;
		}
		if (DEFINITE_DISEASE_SIGNS.matcher(scrubbed).find()) {
			return true;
		}
		if (NORMAL_VARIATION.matcher(text).find()) {
			return false;
		}
		return PATHOLOGICAL_COLORATION.matcher(scrubbed).find();
	}

	public static boolean hasExplicitNoAbnormality(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		if (!EXPLICIT_NO_ABNORMALITY.matcher(text).find()) {
			return false;
		}
		return !hasPositiveAbnormalityAfterNegation(text);
	}

	public static boolean hasCredibleAbnormalityEvidence(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(text);
		if (assessment.allCategoriesExplicitlyNegative() && !hasPositiveAbnormalityAfterNegation(text)) {
			return false;
		}
		if (hasExplicitNoAbnormality(text) && !hasPositiveAbnormalityAfterNegation(text)) {
			return false;
		}
		return hasCrediblePestEvidence(text)
				|| hasCredibleDiseaseEvidence(text)
				|| hasCrediblePhysicalDamageEvidence(text)
				|| hasCredibleAbioticEvidence(text);
	}

	private static boolean hasCredibleAbioticEvidence(String text) {
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(text);
		if (assessment.abioticSectionSeen()) {
			if (assessment.abioticStressState() == VisionObservationAssessment.CategoryState.ABSENT) {
				return false;
			}
		} else if (!assessment.canUseFreeTextEvidence()) {
			return false;
		}
		return ABIOTIC_EVIDENCE.matcher(stripNegatedClauses(assessment.observationText())).find();
	}

	public static VisionObservationAssessment parseVisionAssessment(String visionText) {
		return VisionObservationAssessment.parse(visionText);
	}

	/**
	 * Vision/synthesis explicitly reports no meaningful abnormality and image is assessable.
	 * KB associations alone must not override this.
	 */
	public static boolean supportsHealthyConclusion(String vision, String symptoms, String solution) {
		if (hasInsufficientImageEvidence(vision)) {
			return false;
		}
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(vision);
		if (vision != null && !vision.isBlank() && assessment.isIncompleteHealthAssessment()) {
			return false;
		}
		if (assessment.allCategoriesExplicitlyNegative()
				&& !hasPhysicalDamageEvidence(vision)
				&& !hasPestEvidence(vision)
				&& !hasDiseaseEvidence(vision)
				&& !hasAbioticEvidence(vision)) {
			return true;
		}
		if (hasAmbiguousAbnormalityLanguage(vision, symptoms)) {
			return false;
		}
		if (hasCredibleAbnormalityEvidence(vision)) {
			return false;
		}
		if (vision == null || vision.isBlank()) {
			return claimsNoProblems(symptoms, solution, "");
		}
		return false;
	}

	public static boolean hasIncompleteVisionAssessment(String visionText) {
		if (visionText == null || visionText.isBlank()) {
			return false;
		}
		return VisionObservationAssessment.parse(visionText).isIncompleteHealthAssessment();
	}

	public static boolean hasAmbiguousAbnormalityLanguage(String vision, String symptoms) {
		String combined = nullToEmpty(vision) + " " + nullToEmpty(symptoms);
		if (!AMBIGUOUS_ABNORMALITY.matcher(combined).find()) {
			return false;
		}
		return !hasExplicitNoAbnormality(combined) && !hasCredibleAbnormalityEvidence(combined);
	}

	private static boolean hasPositiveAbnormalityAfterNegation(String text) {
		String scrubbed = stripNegatedClauses(text);
		return PEST_EVIDENCE.matcher(scrubbed).find()
				|| PHYSICAL_DAMAGE_EVIDENCE.matcher(scrubbed).find()
				|| DEFINITE_DISEASE_SIGNS.matcher(scrubbed).find()
				|| ABIOTIC_EVIDENCE.matcher(scrubbed).find();
	}

	public static boolean hasAbioticEvidence(String text) {
		return ABIOTIC_EVIDENCE.matcher(stripNegatedClauses(text)).find();
	}

	public static boolean hasAnyAbnormalityEvidence(String text) {
		return hasCredibleAbnormalityEvidence(text);
	}

	public static boolean hasUncertainPestLanguage(String text) {
		return UNCERTAIN_PEST.matcher(stripNegatedClauses(text)).find();
	}

	public static boolean hasInsufficientImageEvidence(String text) {
		return INSUFFICIENT_IMAGE.matcher(nullToEmpty(text)).find();
	}

	public static boolean claimsNoProblems(String symptoms, String solution, String confidenceNote) {
		String blob = nullToEmpty(symptoms) + " " + nullToEmpty(solution) + " " + nullToEmpty(confidenceNote);
		return CLAIMS_NO_PROBLEMS.matcher(blob).find();
	}

	public static boolean suggestsHealthyCare(String solution) {
		return HEALTHY_CARE.matcher(nullToEmpty(solution)).find();
	}

	/** Evidence-backed symptom summary from vision text when synthesis denies or omits visible problems. */
	public static String summarizeAbnormalitySymptomsFromVision(String visionText) {
		if (visionText == null || visionText.isBlank()) {
			return "Visible plant-health abnormality observed";
		}
		boolean pest = hasCrediblePestEvidence(visionText);
		boolean uncertainPest = hasUncertainPestLanguage(visionText);
		if (pest && !uncertainPest) {
			return summarizeDefinitePestSymptoms(visionText);
		}
		if (pest) {
			return "Unusual attached or clustered structures on the leaf surface; possible pest infestation but not confirmed";
		}
		if (hasCredibleDiseaseEvidence(visionText)) {
			return "Significant visible leaf abnormalities: lesions, discoloration, or necrotic tissue observed";
		}
		if (hasCrediblePhysicalDamageEvidence(visionText)) {
			return summarizePhysicalDamageSymptoms(visionText);
		}
		if (hasAbioticEvidence(visionText)) {
			return "Visible stress-related tissue changes observed";
		}
		return "Visible plant-health abnormality observed in vision analysis";
	}

	private static String summarizePhysicalDamageSymptoms(String vision) {
		String lower = vision.toLowerCase(Locale.ROOT);
		if (lower.contains("skeleton")) {
			return "Skeletonized or heavily perforated leaf tissue with missing sections between veins";
		}
		if (lower.contains("ragged") || lower.contains("torn") || lower.contains("irregular margin")) {
			return "Ragged or torn leaf margins with missing tissue along the edge";
		}
		return "Multiple holes or missing tissue through the leaf blade";
	}

	private static String summarizeDefinitePestSymptoms(String vision) {
		String lower = vision.toLowerCase(Locale.ROOT);
		if (lower.contains("scale")) {
			return "Numerous small attached scale-like organisms on the leaf surface, concentrated along veins/midrib";
		}
		if (lower.contains("mealybug")) {
			return "White cottony or waxy clusters consistent with mealybugs on the leaf surface";
		}
		if (lower.contains("aphid")) {
			return "Clusters of small insects on the leaf consistent with aphids";
		}
		if (lower.contains("mite") || lower.contains("webbing")) {
			return "Mite-related signs (stippling or webbing) visible on the leaf";
		}
		return "Visible attached insects or pest structures on the leaf surface";
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
