package com.plantdoctor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq")
public class GroqProperties {

	private String apiKey;
	private String baseUrl = "https://api.groq.com/openai/v1";
	private String model;
	private int connectTimeoutMs = 4000;
	private int readTimeoutMs = 8000;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public boolean hasConfiguredApiKey() {
		String key = apiKey == null ? "" : apiKey.trim();
		return !key.isEmpty() && !"your_groq_api_key_here".equalsIgnoreCase(key);
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	public void setConnectTimeoutMs(int connectTimeoutMs) {
		this.connectTimeoutMs = connectTimeoutMs;
	}

	public int getReadTimeoutMs() {
		return readTimeoutMs;
	}

	public void setReadTimeoutMs(int readTimeoutMs) {
		this.readTimeoutMs = readTimeoutMs;
	}
}
