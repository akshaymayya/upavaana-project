package com.plantdoctor.service;

/**
 * Image/vision analysis for the diagnosis pipeline. Implementations: Gemini (MVP active), NVIDIA (retained).
 */
public interface PlantVisionClient {

	String analyzeImage(byte[] imageBytes, String mimeType);
}
