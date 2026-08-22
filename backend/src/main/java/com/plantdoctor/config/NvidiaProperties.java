package com.plantdoctor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nvidia")
public class NvidiaProperties {

	private String apiKey;
	private String baseUrl = "https://integrate.api.nvidia.com/v1";
	private String visionModel = "meta/llama-3.2-11b-vision-instruct";
	private String textModel = "meta/llama-3.1-8b-instruct";
	private String deepseekApiKey;
	private String deepseekBaseUrl = "https://api.deepseek.com/v1";
	private String deepseekModel = "deepseek-v4-pro";

	public String getApiKey() { return apiKey; }
	public void setApiKey(String apiKey) { this.apiKey = apiKey; }

	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

	public String getVisionModel() { return visionModel; }
	public void setVisionModel(String visionModel) { this.visionModel = visionModel; }

	public String getTextModel() { return textModel; }
	public void setTextModel(String textModel) { this.textModel = textModel; }

	public String getDeepseekApiKey() { return deepseekApiKey; }
	public void setDeepseekApiKey(String deepseekApiKey) { this.deepseekApiKey = deepseekApiKey; }

	public String getDeepseekBaseUrl() { return deepseekBaseUrl; }
	public void setDeepseekBaseUrl(String deepseekBaseUrl) { this.deepseekBaseUrl = deepseekBaseUrl; }

	public String getDeepseekModel() { return deepseekModel; }
	public void setDeepseekModel(String deepseekModel) { this.deepseekModel = deepseekModel; }
}
