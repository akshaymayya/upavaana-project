package com.plantdoctor.service;

/** Shared complete vision strings for regression tests. */
final class VisionTestFixtures {

	static final String COMPLETE_HEALTHY_ASSESSMENT =
			"Snake plant. TISSUE DAMAGE: None seen. PEST SURFACE SCAN: None seen. "
					+ "DISEASE SIGNS: None seen. ABIOTIC STRESS: None seen. "
					+ "OVERALL CONCLUSION: Healthy — all categories explicitly negative.";

	static final String COMPLETE_HEALTHY_POTHOS =
			"Epipremnum aureum. TISSUE DAMAGE: None seen. PEST SURFACE SCAN: None seen. "
					+ "DISEASE SIGNS: None seen. ABIOTIC STRESS: None seen. "
					+ "Normal yellow and green variegation. OVERALL CONCLUSION: Healthy.";

	static final String INCOMPLETE_MORPHOLOGY_ONLY =
			"### 1. Leaf Morphology Analysis\n"
					+ "- Leaf shape: broad oval\n"
					+ "- Edge/margin: smooth\n"
					+ "- Texture: thin\n"
					+ "- Venation: pinnate\n"
					+ "Plant identification: unknown houseplant.";

	private VisionTestFixtures() {
	}
}
