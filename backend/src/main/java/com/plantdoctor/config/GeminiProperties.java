package com.plantdoctor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

	private String apiKey;
	private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
	private String model = "gemini-2.5-flash";
	/** Connect timeout for a single Gemini HTTP call. Keep short so fallback can run. */
	private int connectTimeoutMs = 4000;
	/** Read/response timeout for a single Gemini HTTP call. One attempt, then NVIDIA fallback. */
	private int readTimeoutMs = 8000;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public boolean hasConfiguredApiKey() {
		String key = apiKey == null ? "" : apiKey.trim();
		return !key.isEmpty() && !"your_gemini_api_key_here".equalsIgnoreCase(key);
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
