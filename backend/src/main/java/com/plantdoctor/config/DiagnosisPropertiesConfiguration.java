package com.plantdoctor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DiagnosisPropertiesConfiguration {

	private static final Logger log = LoggerFactory.getLogger(DiagnosisPropertiesConfiguration.class);

	@Bean
	@ConfigurationProperties(prefix = "app.diagnosis")
	public DiagnosisProperties diagnosisProperties(Environment environment) {
		DiagnosisProperties properties = new DiagnosisProperties();
		properties.attachEnvironment(environment);
		log.info("ACTIVE_VISION_PROVIDER={} ACTIVE_SYNTHESIS_PROVIDER=groq vision-fallback={}",
				properties.resolvedVisionProvider(),
				properties.resolvedVisionFallbackProvider());
		return properties;
	}

	@Bean
	public ApplicationRunner diagnosisProviderStartupLogger(
			DiagnosisProperties diagnosisProperties,
			GeminiProperties geminiProperties,
			GroqProperties groqProperties) {
		return args -> log.info(
				"ACTIVE_VISION_PROVIDER={} ACTIVE_SYNTHESIS_PROVIDER=groq GEMINI_API_KEY configured={} GROQ_API_KEY configured={}",
				diagnosisProperties.resolvedVisionProvider(),
				geminiProperties.hasConfiguredApiKey(),
				groqProperties.hasConfiguredApiKey());
	}
}
