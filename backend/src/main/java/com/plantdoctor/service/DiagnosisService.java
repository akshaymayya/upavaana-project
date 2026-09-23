package com.plantdoctor.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantdoctor.config.DiagnosisProperties;
import com.plantdoctor.entity.Disease;
import com.plantdoctor.entity.Plant;
import com.plantdoctor.entity.Query;
import com.plantdoctor.repository.DiseaseRepository;
import com.plantdoctor.repository.QueryRepository;
import com.plantdoctor.service.DiseaseCandidate.MatchType;

@Service
public class DiagnosisService {

	private static final Logger log = LoggerFactory.getLogger(DiagnosisService.class);
	private static final String UPLOAD_DIR = "uploads";
	private static final int MIN_SYMPTOM_SCORE = 4;
	private static final int MAX_SYMPTOM_CANDIDATES = 3;
	private static final int MAX_PLANT_NAME_CANDIDATES = 5;

	private final DiseaseRepository diseaseRepository;
	private final QueryRepository queryRepository;
	private final NvidiaClientService nvidiaClientService;
	private final DiagnosisProperties diagnosisProperties;
	private final ObjectMapper objectMapper;

	public DiagnosisService(DiseaseRepository diseaseRepository, QueryRepository queryRepository,
			NvidiaClientService nvidiaClientService, DiagnosisProperties diagnosisProperties) {
		this.diseaseRepository = diseaseRepository;
		this.queryRepository = queryRepository;
		this.nvidiaClientService = nvidiaClientService;
		this.diagnosisProperties = diagnosisProperties;
		this.objectMapper = new ObjectMapper();
	}

	public DiagnosisResult diagnosePlant(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Image file is required");
		}

		String filename = saveImage(file);
		String imageUrl = UPLOAD_DIR + "/" + filename;

		try {
			byte[] imageBytes = file.getBytes();
			String contentType = file.getContentType();
			if (contentType == null) {
				contentType = "image/jpeg";
			}

			String symptomsDescription = nvidiaClientService.analyzeImage(imageBytes, contentType);
			log.info("Vision analysis received ({} chars)",
					symptomsDescription == null ? 0 : symptomsDescription.length());

			List<DiseaseCandidate> candidateDiseases = findCandidateDiseases(symptomsDescription);

			String provider = diagnosisProperties.resolvedProvider();
			log.info("Active synthesis provider={} (os.env={} sysprop={} bound={})", provider,
					System.getenv(DiagnosisProperties.ENV_ACTIVE_SYNTHESIS_PROVIDER),
					System.getProperty(DiagnosisProperties.ENV_ACTIVE_SYNTHESIS_PROVIDER),
					diagnosisProperties.getActiveSynthesisProvider());
			DiagnosisResult diagnosis;
			if (DiagnosisProperties.PROVIDER_OPENAI.equals(provider)) {
				diagnosis = nvidiaClientService.synthesizeDiagnosisWithOpenAi(symptomsDescription, candidateDiseases);
			} else if (DiagnosisProperties.PROVIDER_GROQ.equals(provider)) {
				diagnosis = nvidiaClientService.synthesizeDiagnosisWithGroq(symptomsDescription, candidateDiseases);
			} else {
				diagnosis = nvidiaClientService.synthesizeDiagnosisWithDeepSeek(symptomsDescription, candidateDiseases);
			}

			diagnosis = DiagnosisHealthConsistency.enforce(diagnosis);

			String resultJson = objectMapper.writeValueAsString(diagnosis);
			Query queryRecord = new Query(imageUrl, resultJson);
			queryRepository.save(queryRecord);

			return diagnosis;

		} catch (IOException e) {
			log.error("Failed to read uploaded image bytes", e);
			throw new RuntimeException("Failed to read image file: " + e.getMessage(), e);
		}
	}

	private String saveImage(MultipartFile file) {
		try {
			Path uploadPath = Paths.get(UPLOAD_DIR);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}

			String originalFilename = file.getOriginalFilename();
			String extension = "";
			if (originalFilename != null && originalFilename.contains(".")) {
				extension = originalFilename.substring(originalFilename.lastIndexOf("."));
			}

			String filename = UUID.randomUUID().toString() + extension;
			Path filePath = uploadPath.resolve(filename);
			Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

			log.info("Saved uploaded image to: {}", filePath.toAbsolutePath());
			return filename;
		} catch (IOException e) {
			log.error("Failed to save uploaded file", e);
			throw new RuntimeException("Failed to store upload file: " + e.getMessage(), e);
		}
	}

	List<DiseaseCandidate> findCandidateDiseases(String symptomsDescription) {
		List<Disease> allDiseases = diseaseRepository.findAllWithPlant();
		if (allDiseases.isEmpty()) {
			return List.of();
		}

		String lowerDesc = symptomsDescription.toLowerCase();
		List<DiseaseCandidate> candidates = new ArrayList<>();
		Set<Disease> seenDiseases = new HashSet<>();

		List<DiseaseCandidate> plantNameMatches = new ArrayList<>();
		for (Disease disease : allDiseases) {
			if (matchesPlantName(lowerDesc, disease.getPlant())) {
				plantNameMatches.add(new DiseaseCandidate(disease, MatchType.PLANT_NAME));
				seenDiseases.add(disease);
			}
		}
		int plantNameBeforeCap = plantNameMatches.size();
		if (plantNameMatches.size() > MAX_PLANT_NAME_CANDIDATES) {
			plantNameMatches = new ArrayList<>(plantNameMatches.subList(0, MAX_PLANT_NAME_CANDIDATES));
			seenDiseases.clear();
			for (DiseaseCandidate c : plantNameMatches) {
				seenDiseases.add(c.disease());
			}
		}
		candidates.addAll(plantNameMatches);
		if (plantNameBeforeCap != plantNameMatches.size()) {
			log.info("Plant-name candidates capped from {} to {}", plantNameBeforeCap, plantNameMatches.size());
		}

		List<ScoredDisease> symptomScored = new ArrayList<>();
		for (Disease disease : allDiseases) {
			if (seenDiseases.contains(disease)) {
				continue;
			}
			int score = scoreSymptomMatch(lowerDesc, disease);
			if (score >= MIN_SYMPTOM_SCORE) {
				symptomScored.add(new ScoredDisease(disease, score));
			}
		}

		symptomScored.stream()
				.sorted(Comparator.comparingInt(ScoredDisease::score).reversed())
				.limit(MAX_SYMPTOM_CANDIDATES)
				.forEach(scored -> candidates.add(new DiseaseCandidate(scored.disease(), MatchType.SYMPTOM_PATTERN)));

		if (candidates.isEmpty()) {
			log.info("No plant or symptom-pattern matches in DB. AI will synthesize independently.");
		} else {
			long plantMatches = candidates.stream().filter(c -> c.matchType() == MatchType.PLANT_NAME).count();
			long symptomMatches = candidates.stream().filter(c -> c.matchType() == MatchType.SYMPTOM_PATTERN).count();
			log.info("Matched {} candidate(s): {} plant-name, {} symptom-pattern.",
					candidates.size(), plantMatches, symptomMatches);
		}

		return candidates;
	}

	private boolean matchesPlantName(String lowerDesc, Plant plant) {
		String plantName = plant.getName().toLowerCase();
		if (lowerDesc.contains(plantName)) {
			return true;
		}
		if (plant.getCommonNames() != null) {
			for (String name : plant.getCommonNames().toLowerCase().split(",")) {
				String trimmed = name.trim();
				if (!trimmed.isEmpty() && lowerDesc.contains(trimmed)) {
					return true;
				}
			}
		}
		return false;
	}

	private int scoreSymptomMatch(String lowerDesc, Disease disease) {
		int score = 0;

		String diseaseName = disease.getDiseaseName().toLowerCase();
		if (lowerDesc.contains(diseaseName)) {
			score += 5;
		} else {
			String[] nameWords = diseaseName.split("\\s+");
			if (nameWords.length > 1) {
				boolean allPresent = true;
				for (String word : nameWords) {
					if (word.length() > 2 && !lowerDesc.contains(word)) {
						allPresent = false;
						break;
					}
				}
				if (allPresent) {
					score += 3;
				}
			}
		}

		String symptoms = disease.getSymptoms();
		if (symptoms == null || symptoms.isEmpty()) {
			return score;
		}

		String lowerSymptoms = symptoms.toLowerCase();
		for (String phrase : lowerSymptoms.split(",")) {
			String trimmed = phrase.strip();
			if (trimmed.length() < 8) {
				continue;
			}
			int phraseHits = 0;
			for (String word : trimmed.split("[^a-zA-Z0-9]+")) {
				if (word.length() > 3 && isSymptomKeyword(word) && lowerDesc.contains(word)) {
					phraseHits++;
				}
			}
			if (phraseHits >= 2) {
				score += 3;
			}
		}

		int keywordHits = 0;
		for (String word : lowerSymptoms.split("[^a-zA-Z0-9]+")) {
			if (word.length() > 4 && isSymptomKeyword(word) && lowerDesc.contains(word)) {
				keywordHits++;
			}
		}
		score += Math.min(keywordHits, 4);

		return score;
	}

	private boolean isSymptomKeyword(String word) {
		return word.length() >= 4 && !List.of("with", "from", "that", "this", "they", "have", "some", "them", "then",
				"their", "were", "been", "also", "very", "more", "around", "often", "sometimes", "visible",
				"affected").contains(word);
	}

	private record ScoredDisease(Disease disease, int score) {
	}
}
