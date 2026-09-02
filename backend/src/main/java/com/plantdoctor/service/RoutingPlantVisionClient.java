package com.plantdoctor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.plantdoctor.config.DiagnosisProperties;

/**
 * Routes image analysis to the configured vision provider with optional transient-failure fallback.
 */
@Service
@Primary
public class RoutingPlantVisionClient implements PlantVisionClient {

	private static final Logger log = LoggerFactory.getLogger(RoutingPlantVisionClient.class);

	private final DiagnosisProperties diagnosisProperties;
	private final GeminiPlantVisionClient geminiPlantVisionClient;
	private final NvidiaPlantVisionClient nvidiaPlantVisionClient;

	public RoutingPlantVisionClient(
			DiagnosisProperties diagnosisProperties,
			GeminiPlantVisionClient geminiPlantVisionClient,
			NvidiaPlantVisionClient nvidiaPlantVisionClient) {
		this.diagnosisProperties = diagnosisProperties;
		this.geminiPlantVisionClient = geminiPlantVisionClient;
		this.nvidiaPlantVisionClient = nvidiaPlantVisionClient;
	}

	@Override
	public String analyzeImage(byte[] imageBytes, String mimeType) {
		String primary = diagnosisProperties.resolvedVisionProvider();
		log.info("Active vision provider={} (bound={}), fallback={}",
				primary,
				diagnosisProperties.getActiveVisionProvider(),
				diagnosisProperties.resolvedVisionFallbackProvider());
		long primaryStart = System.currentTimeMillis();
		try {
			String result = VisionEvidenceNormalizer.normalize(callProvider(primary, imageBytes, mimeType));
			log.info("Diagnosis stage=vision provider={} durationMs={} remainingTotalMs={}",
					primary, System.currentTimeMillis() - primaryStart,
					DiagnosisCallContext.current().remainingTotalMs());
			return result;
		} catch (VisionUnavailableException primaryFailure) {
			log.warn("Diagnosis stage=vision provider={} FAILED durationMs={} kind={}",
					primary, System.currentTimeMillis() - primaryStart, primaryFailure.kind());
			String fallback = diagnosisProperties.resolvedVisionFallbackProvider();
			if (fallback == null || fallback.equalsIgnoreCase(primary)) {
				throw primaryFailure;
			}
			DiagnosisLatencyBudget budget = DiagnosisCallContext.current();
			int remainingTotal = budget.remainingTotalMs();
			if (!budget.canStartProviderCall()) {
				log.error("Skipping vision fallback {} — remainingTotalMs={} below useful minimum (overall deadline)",
						fallback, remainingTotal);
				throw primaryFailure;
			}
			log.warn("Activating vision fallback promptly: primary={} kind={} remainingTotalMs={} nvidiaReadMaxMs={} → {}",
					primary, primaryFailure.kind(), remainingTotal, budget.visionFallbackMaxMs(), fallback);
			long fallbackStart = System.currentTimeMillis();
			try {
				String fallbackResult = VisionEvidenceNormalizer.normalize(
						callProvider(fallback, imageBytes, mimeType));
				log.info("Diagnosis stage=vision-fallback provider={} durationMs={} after primary {} ({})",
						fallback, System.currentTimeMillis() - fallbackStart, primary, primaryFailure.kind());
				return fallbackResult;
			} catch (VisionUnavailableException fallbackFailure) {
				log.error("Diagnosis stage=vision-fallback provider={} FAILED durationMs={} kind={} remainingTotalMs={}",
						fallback, System.currentTimeMillis() - fallbackStart, fallbackFailure.kind(),
						DiagnosisCallContext.current().remainingTotalMs());
				throw fallbackFailure;
			}
		}
	}

	private String callProvider(String provider, byte[] imageBytes, String mimeType) {
		if (DiagnosisProperties.VISION_PROVIDER_NVIDIA.equals(provider)) {
			return nvidiaPlantVisionClient.analyzeImage(imageBytes, mimeType);
		}
		return geminiPlantVisionClient.analyzeImage(imageBytes, mimeType);
	}
}
