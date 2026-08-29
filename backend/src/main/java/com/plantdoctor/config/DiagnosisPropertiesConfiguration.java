package com.plantdoctor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DiagnosisPropertiesConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "app.diagnosis")
	public DiagnosisProperties diagnosisProperties(Environment environment) {
		DiagnosisProperties properties = new DiagnosisProperties();
		properties.attachEnvironment(environment);
		return properties;
	}
}
