package com.plantdoctor.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.plantdoctor.entity.Disease;
import com.plantdoctor.service.DiseaseCandidate.MatchType;
import com.plantdoctor.service.VisualDiagnosisSupport.Category;

/**
 * Single post-synthesis pipeline: IMAGE → VISIBLE EVIDENCE → ABNORMALITY CATEGORY → DIAGNOSIS → CONFIDENCE → ACTION.
 * Failure to identify a cause is not evidence that the plant is healthy.
 */
public final class DiagnosisPipeline {

	private static final Logger log = LoggerFactory.getLogger(DiagnosisPipeline.class);

	static final String UNIDENTIFIED = "Unidentified Issue";

	public static final String VISION_UNAVAILABLE = "Vision analysis unavailable";

	public static final String SYNTHESIS_UNAVAILABLE = "Diagnosis synthesis unavailable";

	private static final Pattern HEALTHY_NAME = Pattern.compile("^Healthy$", Pattern.CASE_INSENSITIVE);

	private static final Pattern NO_ISSUE_SYMPTOMS = Pattern.compile(
			"^(none( visible)?|n/?a|nil|no visible( symptoms| damage| issues?)?|no (signs? of )?(disease|pests?|symptoms?|damage|issues?)( observed| visible)?|healthy( looking)?( plant)?).*$",
			Pattern.CASE_INSENSITIVE);

	private static final String HEALTHY_SOLUTION = "Continue current care and maintenance.";

	private static final Pattern INSECTICIDE = Pattern.compile(
			"\\b(neem|insecticide|insecticidal|pesticide|pyrethrin|malathion|imidacloprid|horticultural oil|spinosad)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern LESION_PATTERN = Pattern.compile(
			"\\b(lesion|necrotic|necrosis|halo|blight|concentric|lighter centers?|pale centers?|"
					+ "dark (?:brown )?(?:spot|patch)es?|brown spots?|leaf[- ]spot)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PEST_DISEASE_NAME = Pattern.compile(
			"insect|chewing pest|pest damage|caterpillar|aphid|mite infestation",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern WEAK_VIEW = Pattern.compile(
			"blurry|distant|poorly lit|unclear|insufficient|one distant|limited view|unreliable",
			Pattern.CASE_INSENSITIVE);

	private static final String LABEL_REMINDER =
			"Follow the **product label** for application rate, timing, and repeat intervals — do not use a universal schedule.";

	private static final String DEFINITE_PEST_SOLUTION =
			"Visible structures on the leaf are consistent with a **possible pest infestation**.\n"
					+ "Inspect closely along veins and leaf undersides; isolate from other plants if spreading.\n"
					+ "Remove heavily infested leaves if practical.\n"
					+ "If confirmed, treat with **horticultural oil** or insecticidal soap per the **product label** — do not invent a spray schedule.";

	private static final String UNCERTAIN_PEST_SOLUTION =
			"Unusual small structures are visible on the leaf, but the cause is **not confirmed** from this photo.\n"
					+ "Photograph a close-up along the midrib and underside.\n"
					+ "Do not assume the plant is healthy; monitor and compare with pest-ID references before treating.";

	private static final String DISEASE_MANAGEMENT_SOLUTION =
			"Visible leaf damage is present — this is **not a healthy plant**.\n"
					+ "Remove the worst-affected leaves and discard them away from the plant.\n"
					+ "Improve **airflow** and avoid keeping foliage wet.\n"
					+ "If you use a disease-control product, follow the **product label** — do not invent a spray schedule.";

	private static final String SAFE_LEAF_SPOT_SOLUTION =
			"These marks look more like **leaf-spot damage** than confirmed insect feeding.\n"
					+ "Remove the worst-affected leaves and discard them away from the plant.\n"
					+ "Improve airflow and avoid keeping foliage wet for long periods.\n"
					+ "If you use a disease-control product, **follow the product label** for dose, timing, and plant suitability — do not use a made-up spray schedule.";

	private static final String SAFE_UNKNOWN_SOLUTION =
			"Cause is not confirmed from this photo.\n"
					+ "Remove badly damaged leaves, keep soil and light steady, and photograph a single leaf close-up if it worsens.\n"
					+ "Do **not** start insecticide unless you later see insects, webbing, or clear chewing.";

	private static final String PHYSICAL_DAMAGE_SOLUTION =
			"Visible holes or missing leaf tissue are present — this is **not a healthy plant**.\n"
					+ "Cause is uncertain from this photo (possible insect feeding); no specific pest is confirmed.\n"
					+ "Inspect leaf undersides and new growth for chewing insects or slugs.\n"
					+ "Monitor for additional damage over the next few days.\n"
					+ "Remove severely damaged leaves if practical.\n"
					+ "Identify any pest before targeted treatment — **do not spray insecticide** without confirmed insects.";

	private static final String NUTRIENT_STYLE_SOLUTION =
			"Treat as a **nutrient or cultural** issue, not a pest.\n"
					+ "Check light, watering, and a balanced fertilizer per the plant's needs.\n"
					+ "Follow any product **label**; do not spray insecticide without pest evidence.";

	private static final String ABIOTIC_STYLE_SOLUTION =
			"Treat as **environmental stress** (sun, wind, or water), not a pest or infection, unless other signs appear.\n"
					+ "Move or shield the plant as needed and follow normal care — no insecticide without pest evidence.";

	private static final String MORE_PHOTOS =
			" Low — photo is limited. A close-up of the damaged area, the leaf underside, and the whole plant would help.";

	private DiagnosisPipeline() {
	}

	public static DiagnosisResult enforce(
			DiagnosisResult synthesis,
			String visionText,
			List<DiseaseCandidate> candidates) {
		if (synthesis == null) {
			return null;
		}
		if (VisionUnavailableMarkers.isUnavailableDiagnosis(synthesis)) {
			return synthesis;
		}
		VisualContext ctx = VisualContext.from(visionText, synthesis);
		log.info("Vision assessment state: {}", ctx.assessment().diagnosticSummary());

		if (VisualEvidencePatterns.supportsHealthyConclusion(
				visionText, synthesis.symptomsEvidenceText(), synthesis.solution())) {
			log.info("Vision reports assessable plant with no credible abnormality — resolving Healthy");
			return buildHealthyResult(synthesis);
		}

		DiagnosisResult result = alignVisualEvidence(synthesis, ctx);
		if (!ctx.hasAbnormality()) {
			result = resolveWithoutAbnormality(result, ctx, candidates);
		} else {
			result = mapDiagnosis(result, ctx, candidates);
		}
		result = enforceTreatment(result, ctx, candidates);
		result = enforceConsistency(result, ctx);
		return finalizeHealth(result, ctx);
	}

	/** Stage 1: visible evidence vs synthesis — block false-healthy outcomes. */
	static DiagnosisResult alignVisualEvidence(DiagnosisResult parsed, String visionText) {
		return alignVisualEvidence(parsed, VisualContext.from(visionText, parsed));
	}

	static DiagnosisResult alignVisualEvidence(DiagnosisResult parsed, VisualContext ctx) {
		if (parsed == null || !shouldAlignWithVision(parsed, ctx)) {
			return parsed;
		}
		log.warn(
				"Aligning synthesis with vision evidence: disease_name={} is_healthy={} category={}",
				parsed.disease_name(), parsed.is_healthy(), ctx.category());
		return buildAlignedResult(parsed, ctx);
	}

	/** Stage 2: abnormality category → supported diagnosis; visual evidence overrides weak retrieval. */
	static DiagnosisResult mapDiagnosis(
			DiagnosisResult parsed,
			String visionText,
			List<DiseaseCandidate> candidates) {
		return mapDiagnosis(parsed, VisualContext.from(visionText, parsed), candidates);
	}

	static DiagnosisResult mapDiagnosis(DiagnosisResult parsed, VisualContext ctx, List<DiseaseCandidate> candidates) {
		if (parsed == null) {
			return null;
		}
		if (!ctx.hasAbnormality()) {
			return resolveWithoutAbnormality(parsed, ctx, candidates);
		}
		if (claimsHealthyOutcome(parsed) && !ctx.hasAbnormality()) {
			return parsed;
		}

		DiagnosisResult current = stripInventedSchedule(parsed);
		Category visual = ctx.category();
		String disease = nullToEmpty(current.disease_name()).trim();
		boolean unidentified = VisualDiagnosisSupport.isUnidentifiedOrGeneric(disease);
		boolean mismatch = !unidentified && !VisualDiagnosisSupport.diseaseNameMatchesCategory(disease, visual);

		if (visual == Category.UNIDENTIFIED) {
			return preventKbOnlyUpgrade(current, ctx, candidates);
		}

		if (VisualDiagnosisSupport.isPestCategory(visual) && !ctx.hasPest()) {
			log.warn("Rejecting pest category {} — no credible pest observation in vision", visual);
			return resolveWithoutAbnormality(current, ctx, candidates);
		}
		if ((visual == Category.LEAF_SPOT_DISEASE || visual == Category.PLANT_DISEASE) && !ctx.hasDisease()) {
			log.warn("Rejecting disease category {} — no credible disease observation in vision", visual);
			return resolveWithoutAbnormality(current, ctx, candidates);
		}

		if (visual == Category.UNCERTAIN_PEST) {
			if (!unidentified) {
				return withConfidence(current, ctx, candidates, visual, disease);
			}
			return new DiagnosisResult(
					current.plant_name(),
					UNIDENTIFIED,
					current.symptoms_matched(),
					current.solution(),
					DiagnosisEvidenceMapping.diagnosticConfidenceNote(visual, candidates),
					false);
		}

		DiseaseCandidate kbMatch = bestKbMatch(candidates, visual);
		String mappedName = kbMatch != null && kbMatch.matchType() == MatchType.PLANT_NAME
				? kbMatch.disease().getDiseaseName()
				: VisualDiagnosisSupport.diagnosisName(visual);

		if (unidentified || mismatch) {
			log.warn("Mapping visual evidence to supported diagnosis: visual={} was disease_name={} now={}",
					visual, disease, mappedName);
			current = new DiagnosisResult(
					current.plant_name(),
					mappedName,
					current.symptoms_matched(),
					current.solution(),
					DiagnosisEvidenceMapping.diagnosticConfidenceNote(visual, candidates),
					false);
		} else {
			current = withConfidence(current, ctx, candidates, visual, disease);
		}
		return current;
	}

	/** Stage 3: treatment must match evidence; no invented schedules. */
	static DiagnosisResult enforceTreatment(
			DiagnosisResult parsed,
			String visionText,
			List<DiseaseCandidate> candidates) {
		return enforceTreatment(parsed, VisualContext.from(visionText, parsed), candidates);
	}

	static DiagnosisResult enforceTreatment(DiagnosisResult parsed, VisualContext ctx, List<DiseaseCandidate> candidates) {
		if (parsed == null) {
			return null;
		}

		String evidence = join(ctx.visionText(), parsed.symptomsEvidenceText());
		String scrubbedEvidence = VisualEvidencePatterns.stripNegatedClauses(
				ctx.visionText() + " " + parsed.symptomsEvidenceText());
		boolean strongPest = VisualEvidencePatterns.hasCrediblePestEvidence(
				ctx.visionText(), parsed.symptomsEvidenceText());
		boolean lesionLike = LESION_PATTERN.matcher(
				VisualEvidencePatterns.stripNegatedClauses(
						ctx.visionText() + " " + parsed.symptomsEvidenceText()))
				.find();
		boolean insecticide = INSECTICIDE.matcher(nullToEmpty(parsed.solution())).find();
		boolean inventedSchedule = InventedSchedulePatterns.PATTERN.matcher(nullToEmpty(parsed.solution())).find();
		boolean pestName = pestNamed(parsed.disease_name());
		boolean weakView = WEAK_VIEW.matcher(evidence + " " + nullToEmpty(parsed.confidence_note())).find();

		DiagnosisResult current = parsed;
		boolean changed = false;

		if (insecticide && !strongPest) {
			log.warn("Insecticide/pest-oil without pest evidence — coercing treatment. disease_name={}",
					parsed.disease_name());
			String safer = selectNonPestSolution(scrubbedEvidence, lesionLike);
			String name = current.disease_name();
			if (PEST_DISEASE_NAME.matcher(nullToEmpty(name)).find() && lesionLike) {
				name = "Leaf-spot disease";
			} else if (pestNamed(name)) {
				if (VisualEvidencePatterns.hasCrediblePhysicalDamageEvidence(ctx.visionText()) && !strongPest) {
					name = "Physical leaf damage";
				} else {
					name = lesionLike ? "Leaf-spot disease" : UNIDENTIFIED;
				}
			}
			String note = appendWhyNotPest(current.confidence_note(), lesionLike);
			if (weakView) {
				note = ensureNotHigh(note) + MORE_PHOTOS;
			}
			current = new DiagnosisResult(
					current.plant_name(),
					name,
					current.symptoms_matched(),
					safer,
					appendNote(note, "Visual evidence does not confirm a pest; insecticide was not appropriate."),
					false);
			changed = true;
		} else if (inventedSchedule) {
			log.warn("Invented treatment schedule stripped from solution");
			current = stripInventedSchedule(current);
			changed = true;
		}

		if (weakView && looksOverconfident(current.confidence_note())) {
			current = new DiagnosisResult(
					current.plant_name(),
					current.disease_name(),
					current.symptoms_matched(),
					current.solution(),
					ensureNotHigh(nullToEmpty(current.confidence_note())) + MORE_PHOTOS,
					current.is_healthy());
			changed = true;
		}

		if (!changed && (ctx.visionText() == null || ctx.visionText().isBlank())) {
			return parsed;
		}
		return annotateRetrievalVsVisual(current, candidates, strongPest, lesionLike);
	}

	/** Stage 4: final backstop — healthy cannot ship when vision shows abnormality. */
	static DiagnosisResult finalizeHealth(DiagnosisResult parsed, String visionText) {
		return finalizeHealth(parsed, VisualContext.from(visionText, parsed));
	}

	static DiagnosisResult finalizeHealth(DiagnosisResult parsed, VisualContext ctx) {
		if (parsed == null || !shouldAlignWithVision(parsed, ctx)) {
			return parsed;
		}
		log.warn("Final health backstop: coercing false-healthy outcome. disease_name={}", parsed.disease_name());
		return buildAlignedResult(parsed, ctx);
	}

	// --- shared helpers (also used by legacy guard delegators) ---

	static boolean claimsHealthyOutcome(DiagnosisResult parsed) {
		if (parsed == null) {
			return false;
		}
		if (Boolean.TRUE.equals(parsed.is_healthy())) {
			return true;
		}
		String disease = nullToEmpty(parsed.disease_name()).trim();
		if (HEALTHY_NAME.matcher(disease).matches()) {
			return true;
		}
		return VisualEvidencePatterns.claimsNoProblems(
				parsed.symptomsEvidenceText(), parsed.solution(), parsed.confidence_note())
				|| VisualEvidencePatterns.suggestsHealthyCare(parsed.solution());
	}

	static boolean symptomsDescribeIssue(String symptoms) {
		if (symptoms == null || symptoms.isBlank()) {
			return false;
		}
		return !NO_ISSUE_SYMPTOMS.matcher(symptoms.trim()).matches();
	}

	static boolean looksLikeLesionNotPest(String visionAndSymptoms) {
		String scrubbed = VisualEvidencePatterns.stripNegatedClauses(nullToEmpty(visionAndSymptoms));
		return LESION_PATTERN.matcher(scrubbed).find() && !VisualEvidencePatterns.hasPestEvidence(scrubbed);
	}

	private static boolean shouldAlignWithVision(DiagnosisResult parsed, VisualContext ctx) {
		if (claimsHealthyOutcome(parsed) && ctx.incompleteAssessment()) {
			return true;
		}
		if (claimsHealthyOutcome(parsed) && ctx.insufficientImage()) {
			return true;
		}
		if (claimsHealthyOutcome(parsed) && !ctx.hasAbnormality()) {
			return false;
		}
		if (ctx.assessment().allCategoriesNegative()) {
			return false;
		}
		if (claimsHealthyOutcome(parsed) && ctx.hasAbnormality()) {
			return true;
		}
		if (symptomsDescribeIssue(parsed.symptomsEvidenceText()) && ctx.hasAbnormality()) {
			boolean healthyFlag = Boolean.TRUE.equals(parsed.is_healthy());
			String disease = nullToEmpty(parsed.disease_name()).trim();
			boolean healthyName = disease.equalsIgnoreCase("Healthy")
					|| disease.toLowerCase(Locale.ROOT).contains("no issues")
					|| disease.toLowerCase(Locale.ROOT).contains("no disease");
			return healthyFlag || healthyName;
		}
		return false;
	}

	private static DiagnosisResult buildAlignedResult(DiagnosisResult parsed, VisualContext ctx) {
		if (ctx.insufficientImage() && !ctx.hasAbnormality()) {
			return new DiagnosisResult(
					parsed.plant_name(),
					UNIDENTIFIED,
					"Image quality insufficient for a reliable plant-health assessment",
					"Retake a clear, well-lit close-up of the affected leaves and surrounding healthy tissue.",
					"Diagnostic: Low — insufficient image evidence; cannot confirm health or disease.",
					false);
		}
		if (ctx.incompleteAssessment() && !ctx.hasAbnormality()) {
			return buildIncompleteAssessmentResult(parsed, ctx);
		}

		Category category = ctx.category();
		if (category == Category.UNIDENTIFIED && ctx.hasAbnormality() && !ctx.visionText().isBlank()) {
			category = ctx.hasDisease() ? Category.LEAF_SPOT_DISEASE : Category.PLANT_DISEASE;
		}
		if (category == Category.UNIDENTIFIED && ctx.hasAbnormality() && ctx.visionText().isBlank()) {
			return new DiagnosisResult(
					parsed.plant_name(),
					UNIDENTIFIED,
					parsed.symptoms_matched(),
					"Monitor the plant and photograph a closer view of the affected area.",
					"Diagnostic: Low — symptoms reported but image evidence was not available to confirm.",
					false);
		}

		String diseaseName = category == Category.UNIDENTIFIED || category == Category.UNCERTAIN_PEST
				? UNIDENTIFIED
				: VisualDiagnosisSupport.diagnosisName(category);

		List<String> symptoms = deriveSymptoms(ctx, parsed);
		String solution = deriveSolution(ctx, category);
		String note = deriveAlignmentNote(ctx, category);

		return new DiagnosisResult(parsed.plant_name(), diseaseName, symptoms, solution, note, false);
	}

	private static List<String> deriveSymptoms(VisualContext ctx, DiagnosisResult parsed) {
		String synthesisSymptoms = parsed.symptomsEvidenceText();
		if (symptomsDescribeIssue(synthesisSymptoms)
				&& !VisualEvidencePatterns.claimsNoProblems(synthesisSymptoms, "", "")) {
			return parsed.symptoms_matched();
		}
		if (ctx.hasAbnormality()) {
			return List.of(VisualEvidencePatterns.summarizeAbnormalitySymptomsFromVision(ctx.visionText()));
		}
		if (!parsed.hasSymptomsMatched()) {
			return List.of("Visible symptoms inconsistent with a healthy plant");
		}
		return parsed.symptoms_matched();
	}

	private static String deriveSolution(VisualContext ctx, Category category) {
		if (category == Category.PHYSICAL_LEAF_DAMAGE) {
			return PHYSICAL_DAMAGE_SOLUTION;
		}
		if (ctx.hasPest() && !ctx.hasUncertainPest()) {
			return DEFINITE_PEST_SOLUTION;
		}
		if (ctx.hasPest() && ctx.hasUncertainPest()) {
			return UNCERTAIN_PEST_SOLUTION;
		}
		if (VisualDiagnosisSupport.isPestCategory(category)) {
			return DEFINITE_PEST_SOLUTION;
		}
		return DISEASE_MANAGEMENT_SOLUTION;
	}

	private static String deriveAlignmentNote(VisualContext ctx, Category category) {
		String diagnostic = ctx.hasAbnormality()
				? "Diagnostic: High — visible abnormality present; exact cause may be uncertain."
				: "Diagnostic: Medium — synthesis claimed healthy but outcome was inconsistent.";
		if (category == Category.UNIDENTIFIED || category == Category.UNCERTAIN_PEST) {
			if (ctx.hasUncertainPest()) {
				return "Low — unusual structures visible; pest identification uncertain. A close-up along the vein would help.";
			}
			return diagnostic + " Retrieval: Low (no KB candidate).";
		}
		return diagnostic + " Exact disease/pathogen not confirmed from photo alone.";
	}

	private static DiagnosisResult resolveWithoutAbnormality(
			DiagnosisResult current,
			VisualContext ctx,
			List<DiseaseCandidate> candidates) {
		if (VisualEvidencePatterns.hasAmbiguousAbnormalityLanguage(ctx.visionText(), current.symptomsEvidenceText())) {
			log.info("Ambiguous abnormality language without credible evidence — Unidentified Issue");
			return new DiagnosisResult(
					current.plant_name(),
					UNIDENTIFIED,
					current.symptoms_matched(),
					"Monitor the plant and photograph a closer view of the area of concern.",
					"Diagnostic: Low — possible abnormality mentioned but not confirmed from this photo.",
					false);
		}
		String disease = nullToEmpty(current.disease_name()).trim();
		if (ctx.insufficientImage()) {
			return new DiagnosisResult(
					current.plant_name(),
					UNIDENTIFIED,
					"Image quality insufficient for a reliable plant-health assessment",
					"Retake a clear, well-lit close-up of the affected leaves and surrounding healthy tissue.",
					"Diagnostic: Low — insufficient image evidence; cannot confirm health or disease.",
					false);
		}
		if (ctx.incompleteAssessment() && claimsHealthyOutcome(current)) {
			return buildIncompleteAssessmentResult(current, ctx);
		}
		if (HEALTHY_NAME.matcher(disease).matches() && Boolean.TRUE.equals(current.is_healthy())) {
			if (ctx.incompleteAssessment()) {
				return buildIncompleteAssessmentResult(current, ctx);
			}
			if (ctx.visionText().isBlank() && symptomsDescribeIssue(current.symptomsEvidenceText())) {
				return new DiagnosisResult(
						current.plant_name(),
						UNIDENTIFIED,
						current.symptoms_matched(),
						"Monitor the plant and photograph a closer view of the affected area.",
						"Diagnostic: Low — reported symptoms conflict with a healthy conclusion; image evidence needed.",
						false);
			}
			return current;
		}
		current = preventKbOnlyUpgrade(current, ctx, candidates);
		if (isSpecificDiagnosis(current.disease_name()) || indicatesDiseaseAction(current.solution())) {
			log.warn("Specific diagnosis without credible visual abnormality — coercing Healthy: {}",
					current.disease_name());
			return buildHealthyResult(current);
		}
		if (claimsHealthyOutcome(current)) {
			return buildHealthyResult(current);
		}
		return current;
	}

	private static DiagnosisResult enforceConsistency(DiagnosisResult parsed, VisualContext ctx) {
		if (parsed == null) {
			return null;
		}
		boolean deniesProblems = deniesNoProblems(parsed, ctx);
		if (isSpecificDiagnosis(parsed.disease_name()) && !ctx.hasAbnormality() && deniesProblems) {
			log.warn("Consistency fix: disease diagnosis conflicts with no-symptom evidence — Healthy");
			return buildHealthyResult(parsed);
		}
		if (isSpecificDiagnosis(parsed.disease_name()) && deniesProblems && !symptomsSupportDiagnosis(parsed, ctx)) {
			log.warn("Consistency fix: symptoms deny problems but diagnosis is {} — Healthy", parsed.disease_name());
			return buildHealthyResult(parsed);
		}
		if (indicatesDiseaseAction(parsed.solution()) && deniesProblems && !ctx.hasAbnormality()) {
			log.warn("Consistency fix: disease-management action without abnormality evidence — Healthy");
			return buildHealthyResult(parsed);
		}
		return parsed;
	}

	private static boolean deniesNoProblems(DiagnosisResult parsed, VisualContext ctx) {
		return VisualEvidencePatterns.claimsNoProblems(
				parsed.symptomsEvidenceText(), parsed.solution(), parsed.confidence_note())
				|| VisualEvidencePatterns.hasExplicitNoAbnormality(
						ctx.visionText() + " " + parsed.symptomsEvidenceText());
	}

	private static boolean symptomsSupportDiagnosis(DiagnosisResult parsed, VisualContext ctx) {
		String blob = ctx.visionText() + " " + parsed.symptomsEvidenceText();
		String disease = nullToEmpty(parsed.disease_name()).toLowerCase(Locale.ROOT);
		if (disease.contains("leaf") && disease.contains("spot")) {
			return LESION_PATTERN.matcher(VisualEvidencePatterns.stripNegatedClauses(blob)).find()
					|| VisualEvidencePatterns.hasCredibleDiseaseEvidence(blob);
		}
		if (pestNamed(disease)) {
			return VisualEvidencePatterns.hasCrediblePestEvidence(ctx.visionText());
		}
		return ctx.hasAbnormality();
	}

	private static boolean isSpecificDiagnosis(String diseaseName) {
		if (diseaseName == null || diseaseName.isBlank()) {
			return false;
		}
		if (HEALTHY_NAME.matcher(diseaseName.trim()).matches()) {
			return false;
		}
		return !VisualDiagnosisSupport.isUnidentifiedOrGeneric(diseaseName);
	}

	private static boolean indicatesDiseaseAction(String solution) {
		String lower = nullToEmpty(solution).toLowerCase(Locale.ROOT);
		return lower.contains("not a healthy plant")
				|| lower.contains("visible leaf damage is present")
				|| lower.contains("leaf-spot damage");
	}

	private static DiagnosisResult buildIncompleteAssessmentResult(DiagnosisResult parsed, VisualContext ctx) {
		String missing = String.join(", ", ctx.assessment().missingRequiredSections());
		log.warn("Incomplete vision health assessment — cannot confirm Healthy. Missing: {}", missing);
		return new DiagnosisResult(
				parsed.plant_name(),
				UNIDENTIFIED,
				"Vision analysis did not complete all required health categories (missing: " + missing + ")",
				"Retake a clear photo showing the whole leaf and damaged areas. "
						+ "A complete assessment needs tissue damage, pest scan, disease signs, and abiotic stress.",
				"Diagnostic: Low — incomplete vision assessment; missing sections cannot be treated as 'no symptoms'.",
				false);
	}

	public static DiagnosisResult buildVisionUnavailableResult(VisionUnavailableException failure) {
		log.error("Vision analysis unavailable from provider {} ({})", failure.provider(), failure.kind());
		return new DiagnosisResult(
				"Unknown",
				VISION_UNAVAILABLE,
				"Vision analysis could not be completed — no visual assessment was obtained",
				"Unable to analyze image right now. Please wait a moment and try again.",
				"Diagnostic: Unavailable — " + failure.userFacingDetail() + "; no diagnosis was made.",
				false);
	}

	public static DiagnosisResult buildSynthesisUnavailableResult(String plantNameFromVision, Exception failure) {
		String plantName = StringUtils.hasText(plantNameFromVision) ? plantNameFromVision.trim() : "Unknown";
		if (plantName.length() > 120) {
			plantName = plantName.substring(0, 120);
		}
		String detail = failure == null || failure.getMessage() == null
				? "synthesis provider failed"
				: failure.getMessage();
		log.error("Groq synthesis unavailable: {}", detail);
		return new DiagnosisResult(
				plantName,
				SYNTHESIS_UNAVAILABLE,
				"Visual analysis completed, but a final diagnosis could not be generated",
				"Please try again in a moment. Do not assume the plant is healthy or apply a treatment from this result.",
				"Diagnostic: Unavailable — synthesis failed (" + detail + "); no diagnosis was made.",
				false);
	}

	private static DiagnosisResult buildHealthyResult(DiagnosisResult synthesis) {
		List<String> symptoms = synthesis.symptoms_matched();
		String blob = synthesis.symptomsEvidenceText();
		if (!synthesis.hasSymptomsMatched()
				|| VisualEvidencePatterns.claimsNoProblems(blob, "", "")
				|| NO_ISSUE_SYMPTOMS.matcher(blob.trim()).matches()) {
			symptoms = !synthesis.hasSymptomsMatched() ? List.of("none") : symptoms;
		}
		String solution = VisualEvidencePatterns.suggestsHealthyCare(synthesis.solution())
				? synthesis.solution()
				: HEALTHY_SOLUTION;
		return new DiagnosisResult(
				synthesis.plant_name(),
				"Healthy",
				symptoms,
				solution,
				"Diagnostic: High — vision reports no meaningful abnormality across assessed categories.",
				true);
	}

	private static DiagnosisResult preventKbOnlyUpgrade(
			DiagnosisResult current,
			VisualContext ctx,
			List<DiseaseCandidate> candidates) {
		if (ctx.hasAbnormality() || symptomsDescribeIssue(current.symptomsEvidenceText())) {
			return current;
		}
		String disease = nullToEmpty(current.disease_name()).trim();
		if (VisualDiagnosisSupport.isUnidentifiedOrGeneric(disease) || HEALTHY_NAME.matcher(disease).matches()) {
			return current;
		}
		boolean plantNameOnly = candidates != null && !candidates.isEmpty()
				&& candidates.stream().allMatch(c -> c.matchType() == MatchType.PLANT_NAME);
		if (!plantNameOnly && !isSpecificDiagnosis(disease)) {
			return current;
		}
		log.warn("KB or synthesis disease without visual abnormality — not upgrading: {}", disease);
		return buildHealthyResult(current);
	}

	private static DiagnosisResult withConfidence(
			DiagnosisResult current,
			VisualContext ctx,
			List<DiseaseCandidate> candidates,
			Category visual,
			String diseaseName) {
		if (nullToEmpty(current.confidence_note()).toLowerCase(Locale.ROOT).contains("diagnostic:")) {
			return current;
		}
		Category category = VisualDiagnosisSupport.classifyFromVision(ctx.visionText());
		if (!VisualDiagnosisSupport.diseaseNameMatchesCategory(diseaseName, category)) {
			category = visual;
		}
		return new DiagnosisResult(
				current.plant_name(),
				current.disease_name(),
				current.symptoms_matched(),
				current.solution(),
				DiagnosisEvidenceMapping.diagnosticConfidenceNote(category, candidates),
				current.is_healthy());
	}

	private static DiseaseCandidate bestKbMatch(List<DiseaseCandidate> candidates, Category visual) {
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		for (DiseaseCandidate candidate : candidates) {
			if (candidate.matchType() == MatchType.PLANT_NAME
					&& kbDiseaseMatchesCategory(candidate.disease(), visual)) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean kbDiseaseMatchesCategory(Disease disease, Category visual) {
		if (disease == null) {
			return false;
		}
		String blob = (disease.getDiseaseName() + " " + disease.getSymptoms()).toLowerCase(Locale.ROOT);
		return switch (visual) {
			case SCALE_INFESTATION -> blob.contains("scale");
			case APHID_INFESTATION -> blob.contains("aphid");
			case WHITEFLY_INFESTATION -> blob.contains("whitefly");
			case MEALYBUG_INFESTATION -> blob.contains("mealybug");
			case MITE_INFESTATION -> blob.contains("mite");
			case CHEWING_PEST_DAMAGE -> blob.contains("chew") || blob.contains("hole");
			case PHYSICAL_LEAF_DAMAGE -> blob.contains("hole") || blob.contains("chew") || blob.contains("damage");
			case LEAF_SPOT_DISEASE -> blob.contains("leaf") && blob.contains("spot");
			case PLANT_DISEASE -> blob.contains("disease") || blob.contains("blight") || blob.contains("mildew");
			default -> VisualDiagnosisSupport.isPestCategory(visual) && blob.contains("insect");
		};
	}

	private static String selectNonPestSolution(String scrubbedEvidence, boolean lesionLike) {
		if (lesionLike) {
			return SAFE_LEAF_SPOT_SOLUTION;
		}
		if (VisualEvidencePatterns.hasPhysicalDamageEvidence(scrubbedEvidence)) {
			return PHYSICAL_DAMAGE_SOLUTION;
		}
		String lower = scrubbedEvidence.toLowerCase(Locale.ROOT);
		if (lower.contains("interveinal") || (lower.contains("yellow") && !lower.contains("hole"))) {
			return NUTRIENT_STYLE_SOLUTION;
		}
		if (lower.contains("scorch") || lower.contains("bleach") || lower.contains("crisp brown")) {
			return ABIOTIC_STYLE_SOLUTION;
		}
		return SAFE_UNKNOWN_SOLUTION;
	}

	private static DiagnosisResult stripInventedSchedule(DiagnosisResult parsed) {
		if (!InventedSchedulePatterns.PATTERN.matcher(nullToEmpty(parsed.solution())).find()) {
			return parsed;
		}
		log.warn("Stripping invented treatment schedule from solution");
		String cleaned = InventedSchedulePatterns.PATTERN.matcher(parsed.solution()).replaceAll("").strip();
		if (!cleaned.toLowerCase(Locale.ROOT).contains("label")) {
			cleaned = cleaned + "\n" + LABEL_REMINDER;
		}
		return new DiagnosisResult(
				parsed.plant_name(),
				parsed.disease_name(),
				parsed.symptoms_matched(),
				cleaned,
				parsed.confidence_note(),
				parsed.is_healthy());
	}

	private static DiagnosisResult annotateRetrievalVsVisual(
			DiagnosisResult current,
			List<DiseaseCandidate> candidates,
			boolean strongPest,
			boolean lesionLike) {
		if (nullToEmpty(current.confidence_note()).toLowerCase(Locale.ROOT).contains("diagnostic:")) {
			return current;
		}
		boolean plantName = candidates != null && candidates.stream().anyMatch(c -> c.matchType() == MatchType.PLANT_NAME);
		boolean symptomOnly = candidates != null && !plantName
				&& candidates.stream().anyMatch(c -> c.matchType() == MatchType.SYMPTOM_PATTERN);
		String retrieval = candidates == null || candidates.isEmpty()
				? "Retrieval: Low (no KB candidate)."
				: plantName
						? "Retrieval: High (plant in KB) — not the same as visual disease certainty."
						: symptomOnly
								? "Retrieval: Medium (symptom pattern only — not a confirmed disease)."
								: "Retrieval: mixed.";
		String visual = strongPest
				? "Visual pest evidence: present."
				: lesionLike
						? "Visual: lesion/spot pattern; pest not established."
						: "Visual disease certainty: limited.";
		return new DiagnosisResult(
				current.plant_name(),
				current.disease_name(),
				current.symptoms_matched(),
				current.solution(),
				mergeNotes(current.confidence_note(), retrieval + " " + visual),
				current.is_healthy());
	}

	private static boolean pestNamed(String diseaseName) {
		String d = nullToEmpty(diseaseName).toLowerCase(Locale.ROOT);
		return d.contains("pest") || d.contains("insect") || d.contains("aphid") || d.contains("mite")
				|| d.contains("whitefly") || d.contains("mealybug") || d.contains("scale") || d.contains("chew");
	}

	private static String appendWhyNotPest(String note, boolean lesionLike) {
		String why = lesionLike
				? " Discrete dark lesions with pale centers are more consistent with leaf-spot disease than insect feeding."
				: " Spots, yellowing, or damaged tissue alone are not insect evidence.";
		String base = StringUtils.hasText(note) ? note : "Medium —";
		if (base.toLowerCase(Locale.ROOT).contains("leaf-spot") || base.toLowerCase(Locale.ROOT).contains("lesion")) {
			return ensureNotHigh(base);
		}
		return ensureNotHigh(base) + why;
	}

	private static boolean looksOverconfident(String note) {
		if (!StringUtils.hasText(note)) {
			return true;
		}
		String n = note.toLowerCase(Locale.ROOT);
		return n.startsWith("high") || n.contains("high —") || n.contains("definite");
	}

	private static String ensureNotHigh(String note) {
		if (!StringUtils.hasText(note)) {
			return "Low — evidence is limited.";
		}
		String n = note.trim();
		if (n.toLowerCase(Locale.ROOT).startsWith("high")) {
			return "Low" + n.substring(4);
		}
		return n;
	}

	private static String mergeNotes(String existing, String extra) {
		String e = nullToEmpty(existing).trim();
		String lower = e.toLowerCase(Locale.ROOT);
		if (lower.contains("retrieval:") || lower.contains("diagnostic:")) {
			return e;
		}
		return e.isEmpty() ? extra : extra + " " + e;
	}

	private static String appendNote(String existing, String extra) {
		String e = nullToEmpty(existing).trim();
		return e.isEmpty() ? extra : e + " " + extra;
	}

	private static String join(String... parts) {
		StringBuilder sb = new StringBuilder();
		for (String p : parts) {
			if (p != null && !p.isBlank()) {
				sb.append(p).append(' ');
			}
		}
		return sb.toString();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	record VisualContext(
			String visionText,
			VisionObservationAssessment assessment,
			boolean hasAbnormality,
			boolean hasPest,
			boolean hasDisease,
			boolean hasUncertainPest,
			boolean insufficientImage,
			boolean incompleteAssessment,
			Category category) {

		static VisualContext from(String visionText, DiagnosisResult parsed) {
			return from(visionText, parsed == null ? "" : parsed.symptomsEvidenceText());
		}

		static VisualContext from(String visionText, String symptomsText) {
			String vision = nullToEmpty(visionText);
			String symptoms = nullToEmpty(symptomsText);
			VisionObservationAssessment assessment = VisionObservationAssessment.parse(vision);
			boolean hasAbnormality = VisualEvidencePatterns.hasCredibleAbnormalityEvidence(vision);
			if (!hasAbnormality && vision.isBlank()) {
				String scrubbedSymptoms = VisualEvidencePatterns.stripNegatedClauses(symptoms);
				hasAbnormality = VisualEvidencePatterns.hasPestEvidence(scrubbedSymptoms)
						|| VisualEvidencePatterns.hasPhysicalDamageEvidence(scrubbedSymptoms)
						|| VisualEvidencePatterns.hasCredibleDiseaseEvidence(symptoms);
			}
			return new VisualContext(
					vision,
					assessment,
					hasAbnormality,
					VisualEvidencePatterns.hasCrediblePestEvidence(vision, symptoms),
					VisualEvidencePatterns.hasCredibleDiseaseEvidence(vision),
					VisualEvidencePatterns.hasUncertainPestLanguage(vision),
					VisualEvidencePatterns.hasInsufficientImageEvidence(vision),
					!vision.isBlank() && assessment.isIncompleteHealthAssessment(),
					VisualDiagnosisSupport.classifyFromVision(vision));
		}
	}
}
