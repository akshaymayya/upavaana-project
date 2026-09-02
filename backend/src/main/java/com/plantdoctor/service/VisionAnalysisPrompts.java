package com.plantdoctor.service;

/**
 * Shared vision-analysis prompt used by active and legacy vision providers (Gemini, NVIDIA).
 */
public final class VisionAnalysisPrompts {

	public static final String TEXT =
			"You are an expert plant pathologist and botanist. Analyze the uploaded photo carefully.\n\n" +
			"MANDATORY OUTPUT — include ALL sections below using these EXACT labels (do not skip any):\n" +
			"TISSUE DAMAGE: <findings or 'none seen after scanning'>\n" +
			"PEST SURFACE SCAN: <findings or 'none seen after scanning'>\n" +
			"DISEASE SIGNS: <findings or 'none seen'>\n" +
			"ABIOTIC STRESS: <findings or 'none seen'>\n" +
			"OVERALL CONCLUSION: <healthy only if ALL four categories above are explicitly negative AND no holes, missing tissue, ragged chew margins, or other abnormalities were described>\n\n" +
			"First describe LEAF MORPHOLOGY before naming the plant:\n" +
			"- Leaf shape (oval, lanceolate, lobed, compound, etc.)\n" +
			"- Edge/margin (smooth, serrated, wavy)\n" +
			"- Texture (thick/succulent, thin, leathery, fuzzy)\n" +
			"- Venation pattern if visible\n" +
			"- Growth habit clues (woody shrub/tree branch, herbaceous stem, vine, succulent rosette)\n\n" +
			"Then give your best plant identification. Do not default a single palmate/lobed ornamental leaf to rose. " +
			"Broad palmate leaves with 3–5 lobes and a toothed margin are often hibiscus (or similar mallow); rose leaflets are usually smaller and pinnately compound. " +
			"If uncertain, say so honestly — e.g. " +
			"\"possibly hibiscus or another woody ornamental with palmate leaves\" — " +
			"rather than confidently guessing a poor match like lettuce or rose for the wrong leaf type.\n\n" +
			"INDEPENDENT ASSESSMENT — report each category separately (do not infer one from another):\n" +
			"1) TISSUE DAMAGE: holes, missing leaf tissue, chewing, skeletonization, ragged/torn margins, tears, necrosis, spots, yellowing, wilting. " +
			"If any of those are visible, this category is PRESENT — do NOT write 'none seen'. An insect does not need to be visible. " +
			"Do not name a specific pest from holes alone.\n" +
			"2) PEST SURFACE SCAN (required): Inspect the ENTIRE visible leaf surface including midrib, primary and secondary veins, margins, and any visible underside. " +
			"Look for attached or stationary organisms and structures: scale insects, scale-like bumps, mealybugs, aphid clusters, mites, thrips, eggs, larvae, webbing, frass, waxy bumps, shell-like discs, or clustered oval bumps. " +
			"Pests may be present WITHOUT holes, yellowing, or lesions — attached insects can be the only visible problem. " +
			"Absence of holes does NOT mean absence of pests. Absence of discoloration does NOT mean healthy. " +
			"Report what you see, or state that none were seen after this dedicated scan.\n" +
			"3) DISEASE SIGNS: fungal/bacterial spots, mildew, blight patterns — or none seen. "
			+ "NORMAL VARIATION (variegation, cultivar coloration, natural yellow/green/cream marbling, golden or chartreuse patterns) is NOT disease — describe it as normal for the plant unless there is evidence of pathological change (necrotic tissue, spreading chlorosis, lesions with halos, etc.).\n" +
			"4) ABIOTIC STRESS: scorch, bleaching, crisp edges — or none seen.\n" +
			"5) OVERALL: Call the plant healthy ONLY if categories 1–4 are all explicitly ABSENT after thorough inspection. "
			+ "Visible holes, missing tissue, ragged margins, or chewing damage means NOT healthy — even when no insect is seen and the cause is uncertain. "
			+ "Unknown plant identity, failed disease ID, or missing KB match are NOT reasons to call a plant healthy. "
			+ "If lesions, necrosis, yellowing, or other abnormalities are visible, report them even when the cause is uncertain. " +
			"If anything unusual is visible but identification is uncertain, describe it (e.g. \"possible scale-like structures along the midrib\") rather than saying \"no pests\" or \"healthy\".\n\n" +
			"TISSUE vs PEST distinction:\n" +
			"- Necrotic spots, lesions, tan/pale centers within dark spots, halos — disease/discoloration patterns; do NOT call these holes or pinholes.\n" +
			"- True perforations, missing sections, ragged chew margins, skeletonization, or attached organisms — physical/chewing damage or pests. Holes without a visible insect are still tissue damage; say cause uncertain or possible insect feeding — do not invent a pest species.\n\n" +
			"If multiple leaves are visible, examine each individually. Do not give one blanket \"healthy\" summary while skipping surfaces.\n\n" +
			"Finally summarize morphology, plant guess (with uncertainty if needed), per-category findings, and overall conclusion.";

	public static final String STRICT_COMPLETION =
			"The prior response omitted required health-assessment sections. Re-analyze the SAME image.\n" +
			"You MUST output ALL of the following lines with these EXACT labels (one line each, do not omit any):\n" +
			"TISSUE DAMAGE: <holes, tears, chewing, necrosis, spots, yellowing, wilting — or 'none seen after scanning'>\n" +
			"PEST SURFACE SCAN: <attached insects, scale, webbing, etc. — or 'none seen after scanning'>\n" +
			"DISEASE SIGNS: <spots, mildew, blight — or 'none seen'>\n" +
			"ABIOTIC STRESS: <scorch, bleaching — or 'none seen'>\n" +
			"OVERALL CONCLUSION: <healthy ONLY if all four categories are explicitly negative>\n" +
			"Missing or incomplete sections are not acceptable. Do not output morphology-only analysis.";

	/**
	 * NVIDIA fallback only. Llama tokenizes HTML {@code data:} URIs as text, so this must stay
	 * short. Gemini keeps {@link #TEXT}. Same labeled contract; no KB, history, or Groq text.
	 */
	public static final String NVIDIA =
			"Analyze this plant photo only. Output EXACT labels, one line each:\n"
					+ "TISSUE DAMAGE: <holes/missing tissue/chewing/ragged margins/necrosis/spots/yellowing/wilting; if any are visible write them — never 'none seen'>\n"
					+ "PEST SURFACE SCAN: <scale/mealybugs/aphids/mites/eggs/webbing/frass/attached insects on midrib/veins/margins/underside or 'none seen after scanning'>\n"
					+ "DISEASE SIGNS: <spots/mildew/blight or 'none seen'; variegation is not disease>\n"
					+ "ABIOTIC STRESS: <scorch/bleach/crisp edges or 'none seen'>\n"
					+ "OVERALL CONCLUSION: <healthy ONLY if all four are explicitly negative and no holes/missing tissue/ragged damage were described>\n"
					+ "PLANT: <best identification; say if uncertain>\n"
					+ "Independent categories: visible holes/missing tissue/ragged margins = tissue damage PRESENT, even with no insect. "
					+ "Do not invent a pest species from holes alone; say possible feeding or cause uncertain. "
					+ "no holes ≠ no pests; no discoloration ≠ healthy. "
					+ "Unknown ID or missing KB is not healthy. Describe visible abnormalities; do not invent pests or treatments.";

	private VisionAnalysisPrompts() {
	}
}
