package com.plantdoctor.service;

import org.springframework.stereotype.Service;

import com.plantdoctor.config.DiagnosisProperties;

/**
 * Legacy NVIDIA vision provider — retained for comparison and optional reactivation via configuration.
 */
@Service
public class NvidiaPlantVisionClient implements PlantVisionClient {

	private final NvidiaClientService nvidiaClientService;

	public NvidiaPlantVisionClient(NvidiaClientService nvidiaClientService) {
		this.nvidiaClientService = nvidiaClientService;
	}

	@Override
	public String analyzeImage(byte[] imageBytes, String mimeType) {
		try {
			return nvidiaClientService.analyzeImage(imageBytes, mimeType);
		} catch (VisionUnavailableException unavailable) {
			throw unavailable;
		} catch (Exception ex) {
			VisionUnavailableException unavailable = VisionFailureSupport.toUnavailable(
					DiagnosisProperties.VISION_PROVIDER_NVIDIA, ex);
			if (unavailable != null) {
				throw unavailable;
			}
			throw new VisionUnavailableException(
					DiagnosisProperties.VISION_PROVIDER_NVIDIA,
					VisionFailureKind.UNKNOWN,
					"Error analyzing image via NVIDIA NIM: " + ex.getMessage(),
					ex);
		}
	}
}
