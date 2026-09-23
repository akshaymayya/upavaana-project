package com.plantdoctor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nvidia")
public class NvidiaProperties {

	private String apiKey;
	private String baseUrl = "https://integrate.api.nvidia.com/v1";
	private String visionModel = "meta/llama-3.2-11b-vision-instruct";
	private String textModel = "openai/gpt-oss-20b";
	private String deepseekNimModel = "deepseek-ai/deepseek-v4-pro-0813";

	public String getApiKey() { return apiKey; }
	public void setApiKey(String apiKey) { this.apiKey = apiKey; }

	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

	public String getVisionModel() { return visionModel; }
	public void setVisionModel(String visionModel) { this.visionModel = visionModel; }

	public String getTextModel() { return textModel; }
	public void setTextModel(String textModel) { this.textModel = textModel; }

	public String getDeepseekNimModel() { return deepseekNimModel; }
	public void setDeepseekNimModel(String deepseekNimModel) { this.deepseekNimModel = deepseekNimModel; }
}
