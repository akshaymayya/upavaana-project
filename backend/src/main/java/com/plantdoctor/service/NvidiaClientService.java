package com.plantdoctor.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantdoctor.config.GroqProperties;
import com.plantdoctor.config.NvidiaProperties;
import com.plantdoctor.config.OpenAiProperties;
import com.plantdoctor.entity.Disease;
import com.plantdoctor.service.DiseaseCandidate.MatchType;

@Service
public class NvidiaClientService {

	private static final Logger log = LoggerFactory.getLogger(NvidiaClientService.class);

	private final NvidiaProperties nvidiaProperties;
	private final OpenAiProperties openAiProperties;
	private final GroqProperties groqProperties;
	private final RestTemplate restTemplate;
	private final RestTemplate deepSeekRestTemplate;
	private final RestTemplate openAiRestTemplate;
	private final ObjectMapper objectMapper;

	private static final String GPT_4O = "gpt-4o";

	static final String VISION_ANALYSIS_PROMPT =
			"You are an expert plant pathologist and botanist. Analyze the uploaded photo carefully.\n\n" +
			"First describe LEAF MORPHOLOGY before naming the plant:\n" +
			"- Leaf shape (oval, lanceolate, lobed, compound, etc.)\n" +
			"- Edge/margin (smooth, serrated, wavy)\n" +
			"- Texture (thick/succulent, thin, leathery, fuzzy)\n" +
			"- Venation pattern if visible\n" +
			"- Growth habit clues (woody shrub/tree branch, herbaceous stem, vine, succulent rosette)\n\n" +
			"Then give your best plant identification. Do not default a single palmate/lobed ornamental leaf to rose. " +
			"Broad palmate leaves with 3–5 lobes and a toothed margin are often hibiscus (or similar mallow); rose leaflets are usually smaller and pinnately compound. " +
			"If uncertain, say so honestly — e.g. " +
			"\"possibly hibiscus or another woody ornamental with palmate leaves\" — " +
			"rather than confidently guessing a poor match like lettuce or rose for the wrong leaf type.\n\n" +
			"SMALL-SCALE DAMAGE (required scan):\n" +
			"- Look carefully across the entire visible leaf surface of every leaf in frame — not only large, obvious symptoms.\n" +
			"- Search for small holes, pinholes, chew marks, ragged edges, stippling, or other minor pest/mechanical damage, even if only a few marks on one leaf.\n" +
			"- If multiple leaves or plants are visible, examine each visible leaf individually. Report findings per leaf (or say which leaves look clear vs damaged). Do not give one blanket \"the plant looks healthy\" summary while skipping leaves.\n" +
			"- Do not conclude healthy / no disease / no pests unless you have actually scanned for this small-scale damage. Large clean areas do not prove the photo is healthy if you have not inspected margins and the rest of the visible surface.\n\n" +
			"Finally list all visible symptoms in detail (color, pattern, location on leaf): " +
			"spots, lesions, halos, yellowing, wilting, mold, pests, small holes, chew marks, etc.\n\n" +
			"Write a cohesive description covering morphology, plant guess (with uncertainty if needed), per-leaf or per-region findings, and symptoms.";

	static final String SYNTHESIS_EVIDENCE_AND_FORMAT =
			"COMMIT: One disease_name only. Do not blend two theories into disease_name or as equal-weight steps in solution.\n" +
			"EVIDENCE WEIGHT: Specific unambiguous vision (holes, chew marks, visible insects, ragged chewing) outranks generic multi-cause signs (yellowing, wilting). " +
			"If both are present, they DISAGREE — pick the pest/chew diagnosis and treatment. Do not treat yellowing as a second equal cause. " +
			"Do not copy a wilt/fertilizer/pH DB essay as the main plan when vision shows chewing/holes. Weaker alternatives belong only in confidence_note.\n" +
			"SOLUTION FORMAT: Lead with one clear sentence for the primary action. Put real newline characters between steps inside the JSON string (not a single paragraph). Wrap the key treatment phrase in double asterisks, e.g. **neem oil spray**. " +
			"If disease_name is Healthy, keep solution short (continue current care) with no pest lead. Do not wrap the JSON object in markdown fences.\n";

	static final String SYNTHESIS_SYSTEM_PROMPT =
			"You are a world-class plant pathologist and botanist with deep expertise in plant diseases, pests, and treatments.\n" +
			"Your job is to produce an accurate, actionable plant diagnosis in strict JSON format.\n\n" +
			"STRICT RULES:\n" +
			"1. PLANT NAME: Always take the plant name from the Vision Analysis. Never rename or guess a different plant.\n" +
			"2. PLANT_NAME_MATCH records: Use when both the plant and symptoms align with the Vision Analysis. Base treatment on the DB solution.\n" +
			"3. SYMPTOM_PATTERN_MATCH records: The DB plant may differ from the photographed plant. If symptoms closely match, you MAY diagnose using that disease name and solution. " +
			"In confidence_note, explain that symptoms are consistent with this disease pattern seen across many species, but the exact plant type was not confirmed in our database.\n" +
			"4. NO DB MATCH: If no record fits, set disease_name to 'Unidentified Issue', is_healthy to false, and give generic care advice labeled as not from curated research. Never use Healthy as a stand-in for a missing KB row.\n" +
			"5. HEALTHY PLANT: Set disease_name to 'Healthy' and is_healthy to true ONLY if Vision Analysis reports no visible symptoms at all. If vision describes damage, holes, discoloration, pests, or similar, is_healthy must be false. If symptoms_matched describes an issue, disease_name cannot be Healthy.\n" +
			"6. OUTPUT: Respond with ONLY a valid raw JSON object. No code fences around the JSON. Double-asterisk bold is required inside solution string values for the key action.\n" +
			"7. SPECIFIC: disease_name must be a concrete disease or pest when symptoms allow (e.g. chewing pest damage), not vague 'fungal infection'.\n" +
			"8. " + SYNTHESIS_EVIDENCE_AND_FORMAT + "\n" +
			"Required JSON fields:\n" +
			"{\n" +
			"  \"plant_name\": \"exact plant name from vision analysis\",\n" +
			"  \"disease_name\": \"disease or pest name, or 'Healthy', or 'Unidentified Issue'\",\n" +
			"  \"symptoms_matched\": \"specific symptoms you identified from the photo description\",\n" +
			"  \"solution\": \"primary action then newline-separated short steps with **key phrase**\",\n" +
			"  \"confidence_note\": \"High/Medium/Low — brief one-sentence reasoning\",\n" +
			"  \"is_healthy\": false\n" +
			"}";

	public NvidiaClientService(NvidiaProperties nvidiaProperties, OpenAiProperties openAiProperties,
			GroqProperties groqProperties) {
		this.nvidiaProperties = nvidiaProperties;
		this.openAiProperties = openAiProperties;
		this.groqProperties = groqProperties;
		this.objectMapper = new ObjectMapper();
		
		// NVIDIA vision can exceed 25s when the hosted NIM is queued (2026-08-27: dual 25s timeouts, ~52s fail, DeepSeek never ran).
		SimpleClientHttpRequestFactory fastFactory = new SimpleClientHttpRequestFactory();
		fastFactory.setConnectTimeout(25000);
		fastFactory.setReadTimeout(60000);
		this.restTemplate = new RestTemplate(fastFactory);

		// Fail NIM DeepSeek quickly so Groq/text fallback can run; 90s stalls the mobile client.
		SimpleClientHttpRequestFactory deepSeekFactory = new SimpleClientHttpRequestFactory();
		deepSeekFactory.setConnectTimeout(15000);
		deepSeekFactory.setReadTimeout(25000);
		this.deepSeekRestTemplate = new RestTemplate(deepSeekFactory);

		SimpleClientHttpRequestFactory openAiFactory = new SimpleClientHttpRequestFactory();
		openAiFactory.setConnectTimeout(25000);
		openAiFactory.setReadTimeout(60000);
		this.openAiRestTemplate = new RestTemplate(openAiFactory);
	}

	/**
	 * Calls the Nvidia Vision-capable model to get a description of the plant and symptoms.
	 */
	public String analyzeImage(byte[] imageBytes, String mimeType) {
		if (!StringUtils.hasText(nvidiaProperties.getApiKey())) {
			throw new IllegalStateException("NVIDIA_API_KEY is not configured.");
		}

		String base64Image = Base64.getEncoder().encodeToString(imageBytes);
		String imageUrl = "data:" + mimeType + ";base64," + base64Image;

		String url = nvidiaProperties.getBaseUrl() + "/chat/completions";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(nvidiaProperties.getApiKey());

		Map<String, Object> textPart = Map.of(
				"type", "text",
				"text", VISION_ANALYSIS_PROMPT
		);

		Map<String, Object> imagePart = Map.of(
				"type", "image_url",
				"image_url", Map.of("url", imageUrl)
		);

		Map<String, Object> message = Map.of(
				"role", "user",
				"content", List.of(textPart, imagePart)
		);

		Map<String, Object> requestBody = Map.of(
				"model", nvidiaProperties.getVisionModel(),
				"messages", List.of(message),
				"max_tokens", 768
		);

		int maxAttempts = 2;
		Exception lastException = null;
		long startTime = System.currentTimeMillis();

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				log.info("Calling Nvidia Vision NIM model (attempt {}/{}): {}", attempt, maxAttempts, nvidiaProperties.getVisionModel());
				long callStart = System.currentTimeMillis();
				HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
				Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
				long callDuration = System.currentTimeMillis() - callStart;
				log.info("Nvidia Vision NIM call succeeded in {} ms (attempt {}/{})", callDuration, attempt, maxAttempts);

				if (response == null) {
					throw new RuntimeException("Received empty response from NVIDIA NIM Vision API.");
				}

				List<?> choices = (List<?>) response.get("choices");
				if (choices == null || choices.isEmpty()) {
					throw new RuntimeException("No choices returned from NVIDIA NIM Vision API.");
				}

				Map<?, ?> choice = (Map<?, ?>) choices.get(0);
				Map<?, ?> responseMessage = (Map<?, ?>) choice.get("message");
				return (String) responseMessage.get("content");

			} catch (Exception ex) {
				lastException = ex;
				long callDuration = System.currentTimeMillis() - startTime;
				log.warn("Nvidia Vision NIM call failed on attempt {}/{} after {} ms total: {}", attempt, maxAttempts, callDuration, ex.getMessage());
				if (attempt < maxAttempts) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("Retry interrupted: " + ie.getMessage(), ie);
					}
				}
			}
		}

		throw new RuntimeException("Error analyzing image via NVIDIA NIM (failed after " + maxAttempts + " attempts): " + lastException.getMessage(), lastException);
	}

	/**
	 * Synthesizes final diagnosis from symptoms description and matching database diseases.
	 */
	public DiagnosisResult synthesizeDiagnosis(String symptomsDescription, List<DiseaseCandidate> candidateDiseases) {
		if (!StringUtils.hasText(nvidiaProperties.getApiKey())) {
			throw new IllegalStateException("NVIDIA_API_KEY is not configured.");
		}

		String url = nvidiaProperties.getBaseUrl() + "/chat/completions";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(nvidiaProperties.getApiKey());

		String diseasesText = formatCandidateSection(candidateDiseases);

		String systemPrompt = "You are a strict plant pathologist assistant. Follow these rules exactly:\n" +
				"\n" +
				"RULE 1 — PLANT IDENTITY: The plant_name you output MUST come from the Vision Analysis description. Do not invent or rename the plant.\n" +
				"RULE 2 — PLANT + SYMPTOM MATCH: For candidates labeled PLANT_NAME_MATCH, use the disease only if symptoms also align with the Vision Analysis.\n" +
				"RULE 3 — SYMPTOM PATTERN MATCH: For candidates labeled SYMPTOM_PATTERN_MATCH, the plant in the DB may differ from the photo. " +
				"If symptoms closely match, you MAY use that disease name and solution. In confidence_note, state that the pattern is consistent across species " +
				"but the exact plant type could not be confirmed in our database.\n" +
				"RULE 4 — NO MATCH: If no candidate fits, set disease_name to 'Unidentified Issue', is_healthy to false, and give generic care advice. Do NOT set Healthy just because the Knowledge Base missed.\n" +
				"RULE 5 — HEALTHY: Set disease_name to 'Healthy' and is_healthy to true ONLY if Vision Analysis reports no visible symptoms at all (no holes, tears, discoloration, pests, lesions, chew marks). If vision or symptoms_matched describes any issue, disease_name must not be Healthy and is_healthy must be false — use Unidentified Issue when no KB disease fits.\n" +
				"RULE 6 — OUTPUT: Return ONLY a raw JSON object. No code fences around the JSON.\n" +
				SYNTHESIS_EVIDENCE_AND_FORMAT +
				"\n" +
				"Output JSON fields (all required):\n" +
				"{\n" +
				"  \"plant_name\": \"Exact plant name from Vision Analysis\",\n" +
				"  \"disease_name\": \"Matched disease name, or 'Unidentified Issue', or 'Healthy'\",\n" +
				"  \"symptoms_matched\": \"Specific symptoms visible in the photo\",\n" +
				"  \"solution\": \"Primary action, then newline-separated short steps with **key phrase**\",\n" +
				"  \"confidence_note\": \"High / Medium / Low + one-sentence reason\",\n" +
				"  \"is_healthy\": false\n" +
				"}";

		String userPrompt = String.format(
				"=== Vision Analysis (trust this for plant identification) ===\n%s\n\n" +
				"=== Candidate Diseases from Database ===\n%s\n\n" +
				"Apply all rules and return the diagnosis JSON.\n" + SYNTHESIS_EVIDENCE_AND_FORMAT,
				symptomsDescription,
				diseasesText.isEmpty() ? "(No matching diseases found in database. If vision lists symptoms, use Unidentified Issue / is_healthy false — never Healthy.)" : diseasesText
		);

		Map<String, Object> systemMessage = Map.of(
				"role", "system",
				"content", systemPrompt
		);

		Map<String, Object> userMessage = Map.of(
				"role", "user",
				"content", userPrompt
		);

		Map<String, Object> requestBody = Map.of(
				"model", nvidiaProperties.getTextModel(),
				"messages", List.of(systemMessage, userMessage),
				"max_tokens", 2048
		);

		int maxAttempts = 2;
		Exception lastException = null;
		long startTime = System.currentTimeMillis();

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				log.info("Calling Nvidia Text NIM model (attempt {}/{}): {}", attempt, maxAttempts, nvidiaProperties.getTextModel());
				long callStart = System.currentTimeMillis();
				HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
				Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
				long callDuration = System.currentTimeMillis() - callStart;
				log.info("Nvidia Text NIM call succeeded in {} ms (attempt {}/{})", callDuration, attempt, maxAttempts);

				if (response == null) {
					throw new RuntimeException("Received empty response from NVIDIA NIM Text API.");
				}

				List<?> choices = (List<?>) response.get("choices");
				if (choices == null || choices.isEmpty()) {
					throw new RuntimeException("No choices returned from NVIDIA NIM Text API.");
				}

				Map<?, ?> choice = (Map<?, ?>) choices.get(0);
				Map<?, ?> responseMessage = (Map<?, ?>) choice.get("message");
				String rawContent = (String) responseMessage.get("content");
				String jsonContent = extractJson(rawContent);
				log.debug("Sanitized raw content from text model: {}", jsonContent);
				return deserializeDiagnosis(jsonContent);

			} catch (Exception ex) {
				lastException = ex;
				long callDuration = System.currentTimeMillis() - startTime;
				log.warn("Nvidia Text NIM call failed on attempt {}/{} after {} ms total: {}", attempt, maxAttempts, callDuration, ex.getMessage());
				if (attempt < maxAttempts) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("Retry interrupted: " + ie.getMessage(), ie);
					}
				}
			}
		}

		throw new RuntimeException("Error synthesizing diagnosis via NVIDIA NIM (failed after " + maxAttempts + " attempts): " + lastException.getMessage(), lastException);
	}

	/**
	 * Target D-7/D-8 synthesis: OpenAI gpt-4o with json_schema.
	 * Used when ACTIVE_SYNTHESIS_PROVIDER=openai. Falls back to NVIDIA text; never calls DeepSeek.
	 */
	public DiagnosisResult synthesizeDiagnosisWithOpenAi(String visionDescription, List<DiseaseCandidate> candidateDiseases) {
		if (!StringUtils.hasText(openAiProperties.getApiKey())
				|| "your_openai_api_key_here".equalsIgnoreCase(openAiProperties.getApiKey().trim())) {
			log.warn("OPENAI_API_KEY not set — falling back to NVIDIA text model.");
			return synthesizeDiagnosis(visionDescription, candidateDiseases);
		}

		String baseUrl = openAiProperties.getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://api.openai.com/v1";
		}
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		String url = baseUrl + "/chat/completions";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(openAiProperties.getApiKey());

		String systemPrompt = synthesisSystemPrompt();
		String userPrompt = synthesisUserPrompt(visionDescription, candidateDiseases);

		Map<String, Object> schema = diagnosisResultJsonSchema();
		Map<String, Object> jsonSchema = new HashMap<>();
		jsonSchema.put("name", "diagnosis_result");
		jsonSchema.put("strict", true);
		jsonSchema.put("schema", schema);

		Map<String, Object> responseFormat = new HashMap<>();
		responseFormat.put("type", "json_schema");
		responseFormat.put("json_schema", jsonSchema);

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", GPT_4O);
		requestBody.put("messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", userPrompt)
		));
		requestBody.put("response_format", responseFormat);
		requestBody.put("max_tokens", 2048);

		try {
			log.info("Calling OpenAI synthesis (model {}): DB candidates: {}", GPT_4O, candidateDiseases.size());
			long callStart = System.currentTimeMillis();
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
			Map<?, ?> response = openAiRestTemplate.postForObject(url, entity, Map.class);
			long callDuration = System.currentTimeMillis() - callStart;
			log.info("OpenAI synthesis succeeded in {} ms", callDuration);

			if (response == null) {
				throw new RuntimeException("Empty response from OpenAI API.");
			}
			List<?> choices = (List<?>) response.get("choices");
			if (choices == null || choices.isEmpty()) {
				throw new RuntimeException("No choices in OpenAI response.");
			}
			Map<?, ?> choice = (Map<?, ?>) choices.get(0);
			Object finishReason = choice.get("finish_reason");
			if (finishReason != null) {
				String reason = finishReason.toString();
				if ("length".equals(reason) || "content_filter".equals(reason)) {
					throw new RuntimeException("OpenAI finish_reason=" + reason);
				}
			}
			Map<?, ?> msg = (Map<?, ?>) choice.get("message");
			if (msg == null) {
				throw new RuntimeException("No message in OpenAI choice.");
			}
			String rawContent = (String) msg.get("content");
			String jsonContent = extractJson(rawContent);
			DiagnosisResult parsed = deserializeDiagnosis(jsonContent);
			if (!isCompleteDiagnosis(parsed)) {
				throw new RuntimeException("OpenAI returned incomplete DiagnosisResult.");
			}
			return parsed;
		} catch (Exception ex) {
			log.warn("OpenAI synthesis failed — falling back to NVIDIA text model: {}", ex.getMessage());
			return synthesizeDiagnosis(visionDescription, candidateDiseases);
		}
	}

	/**
	 * Groq OpenAI-compatible synthesis. Used when ACTIVE_SYNTHESIS_PROVIDER=groq.
	 */
	public DiagnosisResult synthesizeDiagnosisWithGroq(String visionDescription, List<DiseaseCandidate> candidateDiseases) {
		String apiKey = groqProperties.getApiKey() == null ? "" : groqProperties.getApiKey().trim();
		if (!StringUtils.hasText(apiKey)
				|| "your_groq_api_key_here".equalsIgnoreCase(apiKey)) {
			log.warn("GROQ_API_KEY not set — falling back to NVIDIA text model.");
			return synthesizeDiagnosis(visionDescription, candidateDiseases);
		}

		String baseUrl = groqProperties.getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://api.groq.com/openai/v1";
		}
		baseUrl = baseUrl.trim();
		while (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		String url = baseUrl + "/chat/completions";
		String configuredModel = groqProperties.getModel();
		String model = StringUtils.hasText(configuredModel)
				? configuredModel.trim()
				: "openai/gpt-oss-120b";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(apiKey);

		String systemPrompt = synthesisSystemPrompt();
		String userPrompt = synthesisUserPrompt(visionDescription, candidateDiseases);

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", model);
		requestBody.put("messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", userPrompt)
		));
		requestBody.put("temperature", 0.2);
		requestBody.put("max_tokens", 2048);
		requestBody.put("response_format", Map.of("type", "json_object"));

		int maxAttempts = 2;
		Exception lastException = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				log.info("Calling Groq synthesis (attempt {}/{}): {} | DB candidates: {}",
						attempt, maxAttempts, model, candidateDiseases.size());
				long callStart = System.currentTimeMillis();
				HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
				Map<?, ?> response = openAiRestTemplate.postForObject(url, entity, Map.class);
				long callDuration = System.currentTimeMillis() - callStart;
				log.info("Groq HTTP {}ms model={} usage {}", callDuration, model, formatUsage(response));

				if (response == null) {
					throw new RuntimeException("Empty response from Groq API.");
				}
				List<?> choices = (List<?>) response.get("choices");
				if (choices == null || choices.isEmpty()) {
					throw new RuntimeException("No choices in Groq response.");
				}
				Map<?, ?> choice = (Map<?, ?>) choices.get(0);
				rejectTruncatedChoice(choice, "Groq");
				Map<?, ?> msg = (Map<?, ?>) choice.get("message");
				String rawContent = openAiStyleMessageText(msg);
				String jsonContent = extractJson(rawContent);
				DiagnosisResult parsed = deserializeDiagnosis(jsonContent);
				if (!isCompleteDiagnosis(parsed)) {
					throw new RuntimeException("Groq returned incomplete DiagnosisResult.");
				}
				return parsed;
			} catch (Exception ex) {
				lastException = ex;
				log.warn("Groq call failed on attempt {}/{}: {}", attempt, maxAttempts, ex.getMessage());
				boolean jsonFormatRejected = ex instanceof HttpStatusCodeException httpEx
						&& httpEx.getStatusCode().value() == 400
						&& requestBody.containsKey("response_format");
				if (attempt < maxAttempts && jsonFormatRejected) {
					requestBody.remove("response_format");
					continue;
				}
				break;
			}
		}
		log.warn("Groq synthesis failed — falling back to NVIDIA text model: {}",
				lastException != null ? lastException.getMessage() : "unknown");
		return synthesizeDiagnosis(visionDescription, candidateDiseases);
	}

	/**
	 * NVIDIA NIM DeepSeek synthesis. Used when ACTIVE_SYNTHESIS_PROVIDER=deepseek.
	 * Does not call api.deepseek.com or OpenRouter.
	 */
	public DiagnosisResult synthesizeDiagnosisWithDeepSeek(String visionDescription, List<DiseaseCandidate> candidateDiseases) {
		String systemPrompt = synthesisSystemPrompt();
		String userPrompt = synthesisUserPrompt(visionDescription, candidateDiseases);
		int promptChars = systemPrompt.length() + userPrompt.length();
		DiagnosisResult nvidiaHosted = synthesizeDiagnosisWithNvidiaHostedDeepSeek(
				systemPrompt, userPrompt, promptChars, candidateDiseases.size());
		if (nvidiaHosted != null) {
			return nvidiaHosted;
		}
		log.warn("NVIDIA-hosted DeepSeek failed — falling back to NVIDIA text model.");
		return synthesizeDiagnosis(visionDescription, candidateDiseases);
	}

	/**
	 * Same DeepSeek family via NVIDIA NIM (NVIDIA_API_KEY). Primary path for provider=deepseek.
	 */
	private DiagnosisResult synthesizeDiagnosisWithNvidiaHostedDeepSeek(
			String systemPrompt, String userPrompt, int promptChars, int candidateCount) {
		if (!StringUtils.hasText(nvidiaProperties.getApiKey())) {
			log.warn("NVIDIA_API_KEY not set — cannot call NVIDIA-hosted DeepSeek.");
			return null;
		}
		String nimModel = nvidiaProperties.getDeepseekNimModel();
		if (!StringUtils.hasText(nimModel)) {
			nimModel = "deepseek-ai/deepseek-v4-pro-0813";
		}
		String base = nvidiaProperties.getBaseUrl();
		if (!StringUtils.hasText(base)) {
			base = "https://integrate.api.nvidia.com/v1";
		}
		base = base.trim();
		while (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		String url = base + "/chat/completions";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(nvidiaProperties.getApiKey());

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", nimModel);
		requestBody.put("messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", userPrompt)
		));
		requestBody.put("max_tokens", 2048);
		requestBody.put("temperature", 0.3);

		try {
			log.info("Calling NVIDIA-hosted DeepSeek {}: promptChars={} candidates={}",
					nimModel, promptChars, candidateCount);
			long callStart = System.currentTimeMillis();
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
			Map<?, ?> response = deepSeekRestTemplate.postForObject(url, entity, Map.class);
			long callDuration = System.currentTimeMillis() - callStart;
			log.info("NVIDIA-hosted DeepSeek HTTP {}ms promptChars={} candidates={} usage {}",
					callDuration, promptChars, candidateCount, formatUsage(response));
			if (response == null) {
				throw new RuntimeException("Empty response from NVIDIA-hosted DeepSeek.");
			}
			List<?> choices = (List<?>) response.get("choices");
			if (choices == null || choices.isEmpty()) {
				throw new RuntimeException("No choices from NVIDIA-hosted DeepSeek.");
			}
			Map<?, ?> choice = (Map<?, ?>) choices.get(0);
			rejectTruncatedChoice(choice, "NVIDIA-hosted DeepSeek");
			Map<?, ?> msg = (Map<?, ?>) choice.get("message");
			String rawContent = openAiStyleMessageText(msg);
			String jsonContent = extractJson(rawContent);
			DiagnosisResult parsed = deserializeDiagnosis(jsonContent);
			if (!isCompleteDiagnosis(parsed)) {
				throw new RuntimeException("NVIDIA-hosted DeepSeek returned incomplete DiagnosisResult.");
			}
			return parsed;
		} catch (Exception ex) {
			log.warn("NVIDIA-hosted DeepSeek failed: {}", ex.getMessage());
			return null;
		}
	}

	private String synthesisSystemPrompt() {
		return SYNTHESIS_SYSTEM_PROMPT;
	}

	private String synthesisUserPrompt(String visionDescription, List<DiseaseCandidate> candidateDiseases) {
		boolean hasDbMatch = !candidateDiseases.isEmpty();
		boolean hasSymptomPatternMatch = candidateDiseases.stream()
				.anyMatch(c -> c.matchType() == MatchType.SYMPTOM_PATTERN);
		String dbSection = hasDbMatch
				? formatCandidateSection(candidateDiseases)
				: "(No matching records found in the database for this plant/symptoms combination.)";
		String matchGuidance = hasDbMatch
				? (hasSymptomPatternMatch
						? "Some records are SYMPTOM_PATTERN_MATCH only. Pick the SINGLE best disease. "
								+ SYNTHESIS_EVIDENCE_AND_FORMAT
						: "DB records found — use the best PLANT_NAME_MATCH as primary treatment. "
								+ SYNTHESIS_EVIDENCE_AND_FORMAT)
				: "No DB records — if Vision Analysis lists any damage or symptoms, output Unidentified Issue with is_healthy false. Do not output Healthy. "
						+ SYNTHESIS_EVIDENCE_AND_FORMAT;
		return String.format(
				"=== VISION ANALYSIS (what the camera saw) ===\n%s\n\n" +
				"=== DATABASE RECORDS ===\n%s\n\n" +
				"%s\n\n" +
				"Now produce the diagnosis JSON following all rules.",
				visionDescription,
				dbSection,
				matchGuidance
		);
	}

	private Map<String, Object> diagnosisResultJsonSchema() {
		Map<String, Object> stringType = Map.of("type", "string");
		Map<String, Object> properties = new HashMap<>();
		properties.put("plant_name", stringType);
		properties.put("disease_name", stringType);
		properties.put("symptoms_matched", stringType);
		properties.put("solution", stringType);
		properties.put("confidence_note", stringType);
		properties.put("is_healthy", Map.of("type", "boolean"));

		Map<String, Object> schema = new HashMap<>();
		schema.put("type", "object");
		schema.put("properties", properties);
		schema.put("required", List.of(
				"plant_name", "disease_name", "symptoms_matched", "solution", "confidence_note", "is_healthy"));
		schema.put("additionalProperties", false);
		return schema;
	}

	private DiagnosisResult deserializeDiagnosis(String jsonContent) throws Exception {
		DiagnosisResult parsed = objectMapper.readValue(jsonContent, DiagnosisResult.class);
		return DiagnosisHealthConsistency.enforce(parsed);
	}

	private boolean isCompleteDiagnosis(DiagnosisResult parsed) {
		if (parsed == null) {
			return false;
		}
		return StringUtils.hasText(parsed.plant_name())
				&& StringUtils.hasText(parsed.disease_name())
				&& StringUtils.hasText(parsed.symptoms_matched())
				&& StringUtils.hasText(parsed.solution())
				&& StringUtils.hasText(parsed.confidence_note())
				&& parsed.is_healthy() != null;
	}

	private String formatCandidateSection(List<DiseaseCandidate> candidateDiseases) {
		return candidateDiseases.stream().map(candidate -> {
			Disease d = candidate.disease();
			String matchLabel = candidate.matchType() == MatchType.PLANT_NAME
					? "PLANT_NAME_MATCH"
					: "SYMPTOM_PATTERN_MATCH (similar pattern — DB plant may differ from photo)";
			return String.format(
					"[%s]\n" +
					"Plant: %s\n" +
					"Common Names: %s\n" +
					"Disease Name: %s\n" +
					"Description: %s\n" +
					"Symptoms: %s\n" +
					"Causes: %s\n" +
					"Solution: %s\n" +
					"---",
					matchLabel,
					d.getPlant().getName(),
					d.getPlant().getCommonNames() != null ? d.getPlant().getCommonNames() : "None",
					d.getDiseaseName(),
					d.getDescription() != null ? d.getDescription() : "",
					d.getSymptoms() != null ? d.getSymptoms() : "",
					d.getCauses() != null ? d.getCauses() : "",
					d.getSolution() != null ? d.getSolution() : ""
			);
		}).collect(Collectors.joining("\n"));
	}

	private void rejectTruncatedChoice(Map<?, ?> choice, String source) {
		if (choice == null) {
			return;
		}
		Object finishReason = choice.get("finish_reason");
		if (finishReason == null) {
			return;
		}
		String reason = finishReason.toString();
		if ("length".equals(reason) || "content_filter".equals(reason)) {
			throw new RuntimeException(source + " finish_reason=" + reason);
		}
	}

	private String formatUsage(Map<?, ?> response) {
		if (response == null || response.get("usage") == null) {
			return "n/a";
		}
		Object usage = response.get("usage");
		if (!(usage instanceof Map<?, ?> usageMap)) {
			return String.valueOf(usage);
		}
		Object prompt = usageMap.get("prompt_tokens");
		Object completion = usageMap.get("completion_tokens");
		Object reasoning = usageMap.get("reasoning_tokens");
		if (reasoning == null && usageMap.get("completion_tokens_details") instanceof Map<?, ?> details) {
			reasoning = details.get("reasoning_tokens");
		}
		return "prompt=" + prompt + " completion=" + completion + " reasoning=" + reasoning;
	}

	private String openAiStyleMessageText(Map<?, ?> msg) {
		if (msg == null) {
			return "";
		}
		Object content = msg.get("content");
		if (content instanceof String text && StringUtils.hasText(text)) {
			return text;
		}
		Object reasoning = msg.get("reasoning");
		if (reasoning instanceof String reasoningText && StringUtils.hasText(reasoningText)) {
			return reasoningText;
		}
		return content instanceof String text ? text : "";
	}

	private String extractJson(String content) {
		if (content == null) return "";
		int firstOpenBrace = content.indexOf('{');
		int lastCloseBrace = content.lastIndexOf('}');
		if (firstOpenBrace >= 0 && lastCloseBrace > firstOpenBrace) {
			return content.substring(firstOpenBrace, lastCloseBrace + 1);
		}
		return content.trim();
	}
}
