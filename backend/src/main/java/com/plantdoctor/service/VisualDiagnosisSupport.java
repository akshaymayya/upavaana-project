package com.plantdoctor.service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Infers a supported broad diagnosis category from vision/symptom text.
 * Does not invent species-level IDs — only evidence-backed categories.
 */
public final class VisualDiagnosisSupport {

	public enum Category {
		SCALE_INFESTATION,
		APHID_INFESTATION,
		WHITEFLY_INFESTATION,
		MEALYBUG_INFESTATION,
		MITE_INFESTATION,
		CHEWING_PEST_DAMAGE,
		PHYSICAL_LEAF_DAMAGE,
		PEST_INFESTATION,
		LEAF_SPOT_DISEASE,
		PLANT_DISEASE,
		UNCERTAIN_PEST,
		UNIDENTIFIED
	}

	private static final Pattern SCALE = Pattern.compile(
			"\\bscale(?:[- ]like)?(?:\\s+insects?)?|\\bscale insects?\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern APHID = Pattern.compile("\\baphids?\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern WHITEFLY = Pattern.compile("\\bwhitefl(?:y|ies)\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern MEALYBUG = Pattern.compile("\\bmealybugs?\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern MITE = Pattern.compile(
			"\\b(spider mites?|mites?|webbing|stippling)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern CHEWING = Pattern.compile(
			"\\b(chew(?:ing|ed)? (?:damage|marks?)|ragged (?:edges?|margins?)|feeding holes?)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PHYSICAL_DAMAGE = Pattern.compile(
			"\\b(holes?|perforations?|missing tissue|torn|tears?|notched|skeletoni[sz]ed|"
					+ "irregular (?:margins?|edges?)|mechanical damage|multiple holes)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern DEFINITE_PEST = Pattern.compile(
			"\\b(colonies?|infestation|heavy|numerous|clusters? of|observed|attached|visible insects?)\\b",
			Pattern.CASE_INSENSITIVE);

	private VisualDiagnosisSupport() {
	}

	public static Category classify(String visionText, String symptomsText) {
		return classifyFromVision(visionText);
	}

	/**
	 * Category inference from vision observations only — never from synthesis symptoms or KB context.
	 */
	public static Category classifyFromVision(String visionText) {
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(visionText);
		if (assessment.allCategoriesExplicitlyNegative()
				&& !VisualEvidencePatterns.hasPhysicalDamageEvidence(visionText)) {
			return Category.UNIDENTIFIED;
		}

		String scrubbed = VisualEvidencePatterns.stripNegatedClauses(assessment.observationText());
		boolean pestDenied = assessment.pestSectionSeen()
				&& assessment.pestScanState() == VisionObservationAssessment.CategoryState.ABSENT;
		boolean pestEvidence = !pestDenied && VisualEvidencePatterns.hasPestEvidence(scrubbed);
		boolean diseaseEvidence = VisualEvidencePatterns.hasCredibleDiseaseEvidence(visionText);
		boolean uncertain = VisualEvidencePatterns.hasUncertainPestLanguage(scrubbed);
		boolean definite = DEFINITE_PEST.matcher(scrubbed).find();

		if (pestEvidence && uncertain && !definite) {
			return Category.UNCERTAIN_PEST;
		}

		if (pestEvidence && (!assessment.pestSectionSeen()
				|| assessment.pestScanState() != VisionObservationAssessment.CategoryState.ABSENT)) {
			if (SCALE.matcher(scrubbed).find()) {
				return Category.SCALE_INFESTATION;
			}
			if (MEALYBUG.matcher(scrubbed).find()) {
				return Category.MEALYBUG_INFESTATION;
			}
			if (APHID.matcher(scrubbed).find()) {
				return Category.APHID_INFESTATION;
			}
			if (WHITEFLY.matcher(scrubbed).find()) {
				return Category.WHITEFLY_INFESTATION;
			}
			if (MITE.matcher(scrubbed).find()) {
				return Category.MITE_INFESTATION;
			}
			if (CHEWING.matcher(scrubbed).find()) {
				return Category.CHEWING_PEST_DAMAGE;
			}
			if (pestEvidence) {
				return Category.PEST_INFESTATION;
			}
		}

		if (!assessment.diseaseSectionSeen()
				|| assessment.diseaseSignsState() != VisionObservationAssessment.CategoryState.ABSENT) {
			if (diseaseEvidence) {
				String lower = scrubbed.toLowerCase(Locale.ROOT);
				if (lower.contains("lesion") || lower.contains("spot") || lower.contains("halo")
						|| lower.contains("blight") || lower.contains("mildew")) {
					return Category.LEAF_SPOT_DISEASE;
				}
				return Category.PLANT_DISEASE;
			}
		}

		if (VisualEvidencePatterns.hasCrediblePhysicalDamageEvidence(visionText)) {
			boolean visibleInsect = !pestDenied && VisualEvidencePatterns.hasPestEvidence(scrubbed);
			if (visibleInsect && (CHEWING.matcher(scrubbed).find() || PHYSICAL_DAMAGE.matcher(scrubbed).find())) {
				return Category.CHEWING_PEST_DAMAGE;
			}
			return Category.PHYSICAL_LEAF_DAMAGE;
		}
		return Category.UNIDENTIFIED;
	}

	public static String diagnosisName(Category category) {
		return switch (category) {
			case SCALE_INFESTATION -> "Scale insect infestation";
			case APHID_INFESTATION -> "Aphid infestation";
			case WHITEFLY_INFESTATION -> "Whitefly infestation";
			case MEALYBUG_INFESTATION -> "Mealybug infestation";
			case MITE_INFESTATION -> "Mite infestation";
			case CHEWING_PEST_DAMAGE -> "Chewing pest damage";
			case PHYSICAL_LEAF_DAMAGE -> "Physical leaf damage";
			case PEST_INFESTATION -> "Pest infestation";
			case LEAF_SPOT_DISEASE -> "Leaf-spot disease";
			case PLANT_DISEASE -> "Possible plant disease";
			case UNCERTAIN_PEST, UNIDENTIFIED -> DiagnosisPipeline.UNIDENTIFIED;
		};
	}

	public static boolean isPestCategory(Category category) {
		return category == Category.SCALE_INFESTATION
				|| category == Category.APHID_INFESTATION
				|| category == Category.WHITEFLY_INFESTATION
				|| category == Category.MEALYBUG_INFESTATION
				|| category == Category.MITE_INFESTATION
				|| category == Category.CHEWING_PEST_DAMAGE
				|| category == Category.PEST_INFESTATION;
	}

	public static boolean diseaseNameMatchesCategory(String diseaseName, Category category) {
		if (diseaseName == null || diseaseName.isBlank()) {
			return false;
		}
		String expected = diagnosisName(category);
		if (expected.equalsIgnoreCase(diseaseName.trim())) {
			return true;
		}
		String lower = diseaseName.toLowerCase(Locale.ROOT);
		return switch (category) {
			case SCALE_INFESTATION -> lower.contains("scale");
			case APHID_INFESTATION -> lower.contains("aphid");
			case WHITEFLY_INFESTATION -> lower.contains("whitefly");
			case MEALYBUG_INFESTATION -> lower.contains("mealybug");
			case MITE_INFESTATION -> lower.contains("mite");
			case CHEWING_PEST_DAMAGE -> lower.contains("chew") || lower.contains("chewing");
			case PHYSICAL_LEAF_DAMAGE -> lower.contains("physical") || lower.contains("hole")
					|| lower.contains("missing tissue") || lower.contains("torn");
			case PEST_INFESTATION -> lower.contains("pest") || lower.contains("insect");
			case LEAF_SPOT_DISEASE -> lower.contains("leaf") && lower.contains("spot");
			case PLANT_DISEASE -> lower.contains("disease") || lower.contains("blight") || lower.contains("mildew");
			default -> false;
		};
	}

	public static boolean isUnidentifiedOrGeneric(String diseaseName) {
		if (diseaseName == null || diseaseName.isBlank()) {
			return true;
		}
		String lower = diseaseName.trim().toLowerCase(Locale.ROOT);
		return lower.equals(DiagnosisPipeline.UNIDENTIFIED.toLowerCase(Locale.ROOT))
				|| lower.equals("possible pest infestation")
				|| lower.equals("pest infestation");
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
