package com.plantdoctor.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

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
import com.plantdoctor.config.DiagnosisProperties;
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
	private final RestTemplate groqRestTemplate;
	private final ObjectMapper objectMapper;

	private static final String GPT_4O = "gpt-4o";

	static final String VISION_ANALYSIS_PROMPT = VisionAnalysisPrompts.TEXT;

	static final String SYNTHESIS_EVIDENCE_AND_FORMAT =
			"COMMIT: One disease_name only. Do not blend two theories into disease_name or as equal-weight steps in solution.\n" +
			"EVIDENCE: Separate what the photo shows (visible signs) from what you infer (cause). Evaluate tissue damage, pest presence, disease signs, and abiotic stress independently — absence of one does NOT prove absence of others (no holes ≠ no pests; no discoloration ≠ healthy).\n" +
			"Spots, necrotic lesions, halos, yellowing, or discolored tissue are NOT pest evidence by themselves — do not call them holes, pinholes, or chew marks unless vision describes true perforations or ragged feeding margins.\n" +
			"PEST TREATMENT: Recommend **horticultural oil**, **neem oil**, insecticide, or insecticidal soap ONLY when vision cites pest-specific evidence: visible attached insects, scale-like colonies, mealybugs, webbing, stippling, leaf mines, frass, caterpillars, or clear chewing/feeding holes with ragged margins. " +
			"Do NOT recommend pest control when evidence points to leaf-spot, blight, mildew, nutrient, or abiotic patterns.\n" +
			"CAUSE MATCH: Treatment must match the supported cause. Lesion/spot patterns → disease-management (remove affected leaves, improve airflow, reduce leaf wetness). Uncertain cause → 'Unidentified Issue' with cautious care — not a default insecticide lead.\n" +
			"UNCERTAINTY: If evidence is ambiguous, say so in confidence_note (Low/Medium). Separate diagnostic confidence (what the photo shows) from retrieval confidence (KB match). Do not invent spray schedules or repeat intervals (e.g. 7–10 days); say follow the **product label** when mentioning any product.\n" +
			"SOLUTION FORMAT: Lead with one clear sentence for the primary action. Put real newline characters between steps inside the JSON string (not a single paragraph). Wrap the key treatment phrase in double asterisks, e.g. **remove affected leaves** or **improve airflow** (pest products only when pest evidence exists). " +
			"If disease_name is Healthy, keep solution short (continue current care) with no pest lead. Do not wrap the JSON object in markdown fences.\n";

	static final String SYNTHESIS_SYSTEM_PROMPT =
			"You are a world-class plant pathologist and botanist with deep expertise in plant diseases, pests, and treatments.\n" +
			"Your job is to produce an accurate, actionable plant diagnosis in strict JSON format.\n\n" +
			"STRICT RULES:\n" +
			"1. PLANT NAME: Always take the plant name from the Vision Analysis. Never rename or guess a different plant.\n" +
			"2. PLANT_NAME_MATCH records: Use when both the plant and symptoms align with the Vision Analysis. Base treatment on the DB solution.\n" +
			"3. SYMPTOM_PATTERN_MATCH records: The DB plant may differ from the photographed plant. If symptoms closely match, you MAY diagnose using that disease name and solution. " +
			"In confidence_note, explain that symptoms are consistent with this disease pattern seen across many species, but the exact plant type was not confirmed in our database.\n" +
			"4. NO DB MATCH: If no KB record fits, do NOT default to 'Unidentified Issue' when Vision Analysis clearly supports a broad category (e.g. scale insect infestation, aphid infestation, leaf-spot disease). Use that supported category with appropriate uncertainty in confidence_note. Reserve 'Unidentified Issue' for genuinely ambiguous or insufficient visual evidence.\n" +
			"5. HEALTHY PLANT: Set disease_name to 'Healthy' and is_healthy to true ONLY when Vision Analysis positively confirms ALL categories are negative: no tissue damage, no pest structures/insects, no disease signs, no abiotic stress — after a thorough scan including leaf surface and veins. "
			+ "Never output Healthy or 'no visible symptoms' when vision describes lesions, necrosis, yellowing, spots, or other abnormalities. Unknown plant species or weak KB retrieval is NOT evidence of health. " +
			"If vision describes attached organisms, scale-like bumps, colonies, or possible pests, is_healthy must be false and symptoms_matched must describe them — never copy \"no pests\" or \"healthy\" when vision listed pest evidence. " +
			"If vision describes damage, holes, discoloration, or similar, is_healthy must be false. If symptoms_matched describes an issue, disease_name cannot be Healthy.\n" +
			"6. OUTPUT: Respond with ONLY a valid raw JSON object. No code fences around the JSON. Double-asterisk bold is required inside solution string values for the key action.\n" +
			"7. SPECIFIC: disease_name should reflect the best-supported cause from vision (e.g. Leaf-spot disease, Scale insect infestation, Aphid infestation). Use 'Unidentified Issue' only when visual evidence is insufficient or ambiguous — weak KB retrieval must not erase strong visual pest or disease evidence. "
			+ "Never diagnose a disease or pest from KB/plant identity alone when vision reports no abnormality. Natural variegation and cultivar coloration are not leaf-spot disease. disease_name, symptoms_matched, solution, and is_healthy must be internally consistent.\n" +
			"8. " + SYNTHESIS_EVIDENCE_AND_FORMAT + "\n" +
			"Required JSON fields:\n" +
			"{\n" +
			"  \"plant_name\": \"exact plant name from vision analysis\",\n" +
			"  \"disease_name\": \"disease or pest name, or 'Healthy', or 'Unidentified Issue'\",\n" +
			"  \"symptoms_matched\": [\"specific symptoms you identified from the photo\"],\n" +
			"  \"solution\": \"primary action then newline-separated short steps with **key phrase**\",\n" +
			"  \"confidence_note\": \"High/Medium/Low — brief one-sentence reasoning\",\n" +
			"  \"is_healthy\": false\n" +
			"}";

	static final String GROQ_SYNTHESIS_SYSTEM_PROMPT =
			"Reply with ONLY a raw JSON object. Types: plant_name string, disease_name string, "
					+ "symptoms_matched JSON array of strings, solution string, confidence_note string, is_healthy boolean.\n"
					+ "Never send symptoms_matched as a single string. Example: "
					+ "\"symptoms_matched\": [\"yellow spots\", \"brown lesions\"]. "
					+ "plant_name, disease_name, solution, and confidence_note must be strings, never arrays.\n"
					+ "Be concise: plant identification, observed issue, confidence, evidence-based reason, and action.\n"
					+ "plant_name comes from vision. Healthy/is_healthy true ONLY when tissue, pests, disease signs, and abiotic stress are all ABSENT. "
					+ "UNKNOWN or missing sections are never Healthy.\n"
					+ "Do not invent causes, pests, treatments, or spray schedules. KB may name a cause only if vision shows a matching abnormality. "
					+ "Natural variegation is not disease. Physical damage is not a confirmed pest unless pests are present.\n"
					+ "confidence_note: High/Medium/Low plus one sentence from image evidence only.";

	public NvidiaClientService(NvidiaProperties nvidiaProperties, OpenAiProperties openAiProperties,
			GroqProperties groqProperties) {
		this.nvidiaProperties = nvidiaProperties;
		this.openAiProperties = openAiProperties;
		this.groqProperties = groqProperties;
		this.objectMapper = new ObjectMapper();
		
		// NVIDIA vision fallback: provider-owned timeouts (not Gemini leftovers).
		SimpleClientHttpRequestFactory fastFactory = new SimpleClientHttpRequestFactory();
		fastFactory.setConnectTimeout(nvidiaProperties.getConnectTimeoutMs());
		fastFactory.setReadTimeout(nvidiaProperties.getReadTimeoutMs());
		this.restTemplate = new RestTemplate(fastFactory);
		log.info("NVIDIA vision default HTTP timeouts: connect={}ms read={}ms (not capped by leftover Gemini time)",
				nvidiaProperties.getConnectTimeoutMs(), nvidiaProperties.getReadTimeoutMs());

		// Fail NIM DeepSeek quickly so Groq/text fallback can run; 90s stalls the mobile client.
		SimpleClientHttpRequestFactory deepSeekFactory = new SimpleClientHttpRequestFactory();
		deepSeekFactory.setConnectTimeout(15000);
		deepSeekFactory.setReadTimeout(25000);
		this.deepSeekRestTemplate = new RestTemplate(deepSeekFactory);

		SimpleClientHttpRequestFactory openAiFactory = new SimpleClientHttpRequestFactory();
		openAiFactory.setConnectTimeout(25000);
		openAiFactory.setReadTimeout(60000);
		this.openAiRestTemplate = new RestTemplate(openAiFactory);

		SimpleClientHttpRequestFactory groqFactory = new SimpleClientHttpRequestFactory();
		groqFactory.setConnectTimeout(groqProperties.getConnectTimeoutMs());
		groqFactory.setReadTimeout(groqProperties.getReadTimeoutMs());
		this.groqRestTemplate = new RestTemplate(groqFactory);
		log.info("Groq HTTP timeouts configured: connect={}ms read={}ms",
				groqProperties.getConnectTimeoutMs(), groqProperties.getReadTimeoutMs());
	}

	private RestTemplate nvidiaVisionClientForBudget() {
		DiagnosisLatencyBudget budget = DiagnosisCallContext.current();
		int connect = nvidiaProperties.getConnectTimeoutMs();
		int read = nvidiaProperties.getReadTimeoutMs();
		log.info("NVIDIA vision per-call timeouts connect={}ms read={}ms remainingTotalMs={} (provider-owned, not leftover Gemini time)",
				connect, read, budget.remainingTotalMs());
		if (!budget.canStartProviderCall()) {
			throw new VisionUnavailableException(
					DiagnosisProperties.VISION_PROVIDER_NVIDIA,
					VisionFailureKind.TIMEOUT,
					"NVIDIA vision skipped — overall diagnosis deadline exhausted");
		}
		return httpClient(connect, read);
	}

	static final int NVIDIA_VISION_MAX_TOKENS = 256;
	static final int NVIDIA_VISION_CONTEXT_LIMIT_TOKENS = 131_072;
	static final int NVIDIA_VISION_MAX_INPUT_TOKENS = 40_000;
	/**
	 * HTML data-URI is tokenized as text. ~24k estimated tokens took ~14s; ~6.6k took ~3s.
	 * Stay near the fast path so the 8s NVIDIA read timeout can complete.
	 */
	static final int NVIDIA_VISION_TARGET_INPUT_TOKENS = 8_000;

	static String nvidiaVisionUserContent(String prompt, String dataUri) {
		return prompt + "\n<img src=\"" + dataUri + "\" />";
	}

	static Map<String, Object> nvidiaVisionRequestBody(String model, String prompt, String dataUri) {
		return Map.of(
				"model", model,
				"messages", List.of(Map.of(
						"role", "user",
						"content", nvidiaVisionUserContent(prompt, dataUri))),
				"max_tokens", NVIDIA_VISION_MAX_TOKENS);
	}

	static RestTemplate httpClient(int connectTimeoutMs, int readTimeoutMs) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Math.max(DiagnosisLatencyBudget.MIN_USEFUL_CALL_MS, connectTimeoutMs));
		factory.setReadTimeout(Math.max(DiagnosisLatencyBudget.MIN_USEFUL_CALL_MS, readTimeoutMs));
		return new RestTemplate(factory);
	}

	static int estimateInputTokens(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		return (text.length() + 3) / 4;
	}

	static NvidiaVisionPayload prepareNvidiaVisionPayload(byte[] imageBytes, String mimeType) {
		if (imageBytes == null || imageBytes.length == 0) {
			throw new VisionUnavailableException(
					DiagnosisProperties.VISION_PROVIDER_NVIDIA,
					VisionFailureKind.UNKNOWN,
					"NVIDIA vision skipped — empty image");
		}
		String prompt = VisionAnalysisPrompts.NVIDIA;
		String sentMime = StringUtils.hasText(mimeType) ? mimeType : "image/jpeg";
		NvidiaVisionPayload last = payloadFor(prompt, imageBytes, sentMime, imageBytes.length);
		if (last.estimatedInputTokens() <= NVIDIA_VISION_TARGET_INPUT_TOKENS) {
			return last;
		}
		int[] edges = { 1024, 768, 512, 384, 256 };
		float[] qualities = { 0.72f, 0.65f, 0.58f, 0.5f, 0.42f };
		for (int i = 0; i < edges.length; i++) {
			byte[] resized = jpegDownscale(imageBytes, edges[i], qualities[i]);
			if (resized == null || resized.length == 0) {
				continue;
			}
			last = payloadFor(prompt, resized, "image/jpeg", imageBytes.length);
			if (last.estimatedInputTokens() <= NVIDIA_VISION_TARGET_INPUT_TOKENS) {
				return last;
			}
		}
		if (last.estimatedInputTokens() > NVIDIA_VISION_MAX_INPUT_TOKENS) {
			throw new VisionUnavailableException(
					DiagnosisProperties.VISION_PROVIDER_NVIDIA,
					VisionFailureKind.UNKNOWN,
					"NVIDIA vision skipped — estimatedInputTokens=" + last.estimatedInputTokens()
							+ " exceeds maxInputTokens=" + NVIDIA_VISION_MAX_INPUT_TOKENS
							+ " (contextLimit=" + NVIDIA_VISION_CONTEXT_LIMIT_TOKENS + ")");
		}
		return last;
	}

	private static NvidiaVisionPayload payloadFor(String prompt, byte[] sent, String mime, int originalBytes) {
		String dataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(sent);
		String userContent = nvidiaVisionUserContent(prompt, dataUri);
		return new NvidiaVisionPayload(
				originalBytes,
				sent.length,
				prompt.length(),
				dataUri.length(),
				dataUri,
				userContent,
				estimateInputTokens(userContent));
	}

	static byte[] jpegDownscale(byte[] imageBytes, int maxEdge, float quality) {
		try {
			BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
			if (src == null) {
				return null;
			}
			int width = src.getWidth();
			int height = src.getHeight();
			double scale = Math.min(1.0, (double) maxEdge / Math.max(width, height));
			int newWidth = Math.max(1, (int) Math.round(width * scale));
			int newHeight = Math.max(1, (int) Math.round(height * scale));
			BufferedImage rgb = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = rgb.createGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, newWidth, newHeight);
			graphics.drawImage(src, 0, 0, newWidth, newHeight, Color.WHITE, null);
			graphics.dispose();
			return writeJpeg(rgb, quality);
		} catch (Exception ex) {
			return null;
		}
	}

	private static byte[] writeJpeg(BufferedImage image, float quality) throws Exception {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
		if (!writers.hasNext()) {
			ByteArrayOutputStream fallback = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", fallback);
			return fallback.toByteArray();
		}
		ImageWriter writer = writers.next();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
			writer.setOutput(ios);
			ImageWriteParam param = writer.getDefaultWriteParam();
			if (param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(Math.max(0.3f, Math.min(0.9f, quality)));
			}
			writer.write(null, new IIOImage(image, null, null), param);
		} finally {
			writer.dispose();
		}
		return output.toByteArray();
	}

	record NvidiaVisionPayload(
			int originalImageBytes,
			int sentImageBytes,
			int promptChars,
			int dataUriChars,
			String dataUri,
			String userContent,
			int estimatedInputTokens) {
	}

	/**
	 * Calls the Nvidia Vision-capable model to get a description of the plant and symptoms.
	 */
	public String analyzeImage(byte[] imageBytes, String mimeType) {
		if (!StringUtils.hasText(nvidiaProperties.getApiKey())) {
			throw new IllegalStateException("NVIDIA_API_KEY is not configured.");
		}

		long buildStart = System.currentTimeMillis();
		NvidiaVisionPayload payload = prepareNvidiaVisionPayload(imageBytes, mimeType);
		Map<String, Object> requestBody = nvidiaVisionRequestBody(
				nvidiaProperties.getVisionModel(),
				VisionAnalysisPrompts.NVIDIA,
				payload.dataUri());
		long buildMs = System.currentTimeMillis() - buildStart;
		log.info(
				"NVIDIA vision payload originalImageBytes={} sentImageBytes={} promptChars={} dataUriChars={} estimatedInputTokens={} targetInputTokens={} maxInputTokens={} buildMs={}",
				payload.originalImageBytes(),
				payload.sentImageBytes(),
				payload.promptChars(),
				payload.dataUriChars(),
				payload.estimatedInputTokens(),
				NVIDIA_VISION_TARGET_INPUT_TOKENS,
				NVIDIA_VISION_MAX_INPUT_TOKENS,
				buildMs);

		String url = nvidiaProperties.getBaseUrl() + "/chat/completions";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(nvidiaProperties.getApiKey());

		int maxAttempts = 1;
		Exception lastException = null;
		long startTime = System.currentTimeMillis();
		RestTemplate visionHttp = nvidiaVisionClientForBudget();

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				log.info("NVIDIA vision request provider=nvidia model={} attempt={}/{} estimatedInputTokens={} readTimeoutMs={}",
						nvidiaProperties.getVisionModel(), attempt, maxAttempts, payload.estimatedInputTokens(),
						nvidiaProperties.getReadTimeoutMs());
				long callStart = System.currentTimeMillis();
				HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
				Map<?, ?> response = visionHttp.postForObject(url, entity, Map.class);
				long httpWaitMs = System.currentTimeMillis() - callStart;
				long parseStart = System.currentTimeMillis();

				if (response == null) {
					throw new RuntimeException("Received empty response from NVIDIA NIM Vision API.");
				}

				List<?> choices = (List<?>) response.get("choices");
				if (choices == null || choices.isEmpty()) {
					throw new RuntimeException("No choices returned from NVIDIA NIM Vision API.");
				}

				Map<?, ?> choice = (Map<?, ?>) choices.get(0);
				Map<?, ?> responseMessage = (Map<?, ?>) choice.get("message");
				String content = (String) responseMessage.get("content");
				long parseMs = System.currentTimeMillis() - parseStart;
				log.info("NVIDIA vision success provider=nvidia model={} httpStatus=200 httpWaitMs={} parseMs={} buildMs={}",
						nvidiaProperties.getVisionModel(), httpWaitMs, parseMs, buildMs);
				return content;

			} catch (Exception ex) {
				lastException = ex;
				long callDuration = System.currentTimeMillis() - startTime;
				Integer httpStatus = null;
				if (ex instanceof HttpStatusCodeException httpEx) {
					httpStatus = httpEx.getStatusCode().value();
				}
				log.warn("NVIDIA vision failed provider=nvidia model={} errorType={} httpStatus={} durationMs={}: {}",
						nvidiaProperties.getVisionModel(),
						ex.getClass().getSimpleName(),
						httpStatus,
						callDuration,
						ex.getMessage());
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

		VisionUnavailableException unavailable = VisionFailureSupport.toUnavailable(
				DiagnosisProperties.VISION_PROVIDER_NVIDIA, lastException);
		if (unavailable != null) {
			throw unavailable;
		}
		throw new VisionUnavailableException(
				DiagnosisProperties.VISION_PROVIDER_NVIDIA,
				VisionFailureKind.UNKNOWN,
				"Error analyzing image via NVIDIA NIM (failed after " + maxAttempts + " attempts): "
						+ (lastException == null ? "unknown" : lastException.getMessage()),
				lastException);
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
				"  \"symptoms_matched\": [\"Specific symptoms visible in the photo\"],\n" +
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
			throw new IllegalStateException("GROQ_API_KEY is not configured.");
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

		String systemPrompt = GROQ_SYNTHESIS_SYSTEM_PROMPT;
		String userPrompt = compactSynthesisUserPrompt(visionDescription, candidateDiseases);
		log.info("Groq prompt sizes: systemChars={} userChars={} candidates={}",
				systemPrompt.length(), userPrompt.length(), candidateDiseases.size());

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", model);
		requestBody.put("messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", userPrompt)
		));
		requestBody.put("temperature", 0.2);
		requestBody.put("max_tokens", 1024);
		requestBody.put("response_format", Map.of("type", "json_object"));

		DiagnosisLatencyBudget budget = DiagnosisCallContext.current();
		int groqConnect = budget.groqConnectTimeoutMs(groqProperties.getConnectTimeoutMs());
		int groqRead = budget.groqReadTimeoutMs(groqProperties.getReadTimeoutMs());
		if (!budget.hasUsefulTime(groqRead)) {
			throw new RuntimeException("Groq synthesis skipped — remaining diagnosis budget exhausted");
		}
		RestTemplate groqHttp = httpClient(groqConnect, groqRead);

		int maxAttempts = 1;
		Exception lastException = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				log.info("Calling Groq synthesis (attempt {}/{}): {} connectTimeout={}ms readTimeout={}ms | DB candidates: {}",
						attempt, maxAttempts, model, groqConnect, groqRead, candidateDiseases.size());
				long callStart = System.currentTimeMillis();
				HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
				Map<?, ?> response = groqHttp.postForObject(url, entity, Map.class);
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
		log.error("Groq synthesis failed after {} attempts — NVIDIA text synthesis is not used: {}",
				maxAttempts, lastException != null ? lastException.getMessage() : "unknown");
		throw new RuntimeException(
				"Error synthesizing diagnosis via Groq (failed after " + maxAttempts + " attempts): "
						+ (lastException == null ? "unknown" : lastException.getMessage()),
				lastException);
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

	static String compactSynthesisUserPrompt(String visionDescription, List<DiseaseCandidate> candidateDiseases) {
		boolean hasDbMatch = candidateDiseases != null && !candidateDiseases.isEmpty();
		String dbSection = hasDbMatch
				? formatCompactCandidateSection(candidateDiseases)
				: "(No KB candidates. Diagnose only from vision. Do not invent an abnormality.)";
		String matchGuidance = hasDbMatch
				? "Use a KB name only if vision shows matching abnormality. Never invent pests or treatments."
				: "No KB match — if vision shows abnormality use Unidentified Issue; never Healthy from missing KB.";
		return "=== STRUCTURED VISION ===\n" + compactVisionEvidence(visionDescription) + "\n\n"
				+ "=== KB CANDIDATES (plant/disease/symptoms only) ===\n" + dbSection + "\n\n"
				+ matchGuidance + "\nReturn diagnosis JSON. symptoms_matched must be a JSON array of strings.";
	}

	static String compactVisionEvidence(String visionDescription) {
		if (visionDescription == null || visionDescription.isBlank()) {
			return "";
		}
		VisionObservationAssessment assessment = VisionObservationAssessment.parse(visionDescription);
		StringBuilder extracted = new StringBuilder();
		extracted.append("coverage: ").append(assessment.diagnosticSummary()).append('\n');
		String firstLine = visionDescription.strip().split("\\R", 2)[0].trim();
		if (!firstLine.isEmpty()) {
			extracted.append("plant: ").append(truncate(firstLine, 180)).append('\n');
		}
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
				"(TISSUE DAMAGE|PEST(?:\\s+SURFACE)?\\s+SCAN|DISEASE SIGNS?|ABIOTIC STRESS|OVERALL(?:\\s+CONCLUSION)?)\\s*:\\s*([^\\n]+)",
				java.util.regex.Pattern.CASE_INSENSITIVE).matcher(visionDescription);
		while (matcher.find()) {
			extracted.append(matcher.group(1).toUpperCase()).append(": ").append(matcher.group(2).trim()).append('\n');
		}
		if (extracted.length() < 80) {
			return truncate(visionDescription, 1200);
		}
		return truncate(extracted.toString().trim(), 1400);
	}

	private static String formatCompactCandidateSection(List<DiseaseCandidate> candidateDiseases) {
		return candidateDiseases.stream().map(candidate -> {
			Disease d = candidate.disease();
			String matchLabel = candidate.matchType() == MatchType.PLANT_NAME
					? "PLANT_NAME_MATCH"
					: "SYMPTOM_PATTERN_MATCH";
			return "[" + matchLabel + "] plant=" + d.getPlant().getName()
					+ " disease=" + d.getDiseaseName()
					+ " symptoms=" + truncate(d.getSymptoms(), 240);
		}).collect(Collectors.joining("\n"));
	}

	private static String truncate(String value, int maxChars) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String trimmed = value.trim();
		if (trimmed.length() <= maxChars) {
			return trimmed;
		}
		return trimmed.substring(0, maxChars) + "…";
	}

	private Map<String, Object> diagnosisResultJsonSchema() {
		Map<String, Object> stringType = Map.of("type", "string");
		Map<String, Object> properties = new HashMap<>();
		properties.put("plant_name", stringType);
		properties.put("disease_name", stringType);
		properties.put("symptoms_matched", Map.of(
				"type", "array",
				"items", stringType));
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
				&& parsed.hasSymptomsMatched()
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
