package com.plantdoctor.seed;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.plantdoctor.entity.Disease;
import com.plantdoctor.entity.Plant;
import com.plantdoctor.repository.DiseaseRepository;
import com.plantdoctor.repository.PlantRepository;

/**
 * Loads plant/disease knowledge from a CSV file into MySQL. Data always comes
 * from the file — nothing is hardcoded here.
 */
@Service
public class PlantDiseaseSeedService {

	private static final List<String> EXPECTED_HEADERS = List.of(
			"plant_name",
			"disease_name",
			"description",
			"symptoms",
			"causes",
			"solution");

	private final PlantRepository plantRepository;
	private final DiseaseRepository diseaseRepository;

	public PlantDiseaseSeedService(PlantRepository plantRepository, DiseaseRepository diseaseRepository) {
		this.plantRepository = plantRepository;
		this.diseaseRepository = diseaseRepository;
	}

	@Transactional
	public SeedResult seedFromCsv(Path csvPath) {
		String pathString = csvPath.toAbsolutePath().normalize().toString();

		if (!Files.isRegularFile(csvPath)) {
			return SeedResult.builder(pathString)
					.addError("CSV file not found: " + pathString)
					.build();
		}

		try {
			validateHeader(csvPath);
		} catch (IOException ex) {
			return SeedResult.builder(pathString).addError("Could not read CSV header: " + ex.getMessage()).build();
		}

		List<PlantDiseaseSeedRow> rows;
		try {
			rows = parseRows(csvPath);
		} catch (IOException | RuntimeException ex) {
			return SeedResult.builder(pathString).addError("Could not parse CSV: " + ex.getMessage()).build();
		}

		int plantsCreated = 0;
		int diseasesInserted = 0;
		int diseasesSkipped = 0;
		List<String> errors = new ArrayList<>();

		int rowNumber = 1;
		for (PlantDiseaseSeedRow row : rows) {
			rowNumber++;
			try {
				RowOutcome outcome = processRow(row);
				plantsCreated += outcome.plantsCreated();
				diseasesInserted += outcome.diseasesInserted();
				diseasesSkipped += outcome.diseasesSkipped();
			} catch (Exception ex) {
				errors.add("Row " + rowNumber + ": " + ex.getMessage());
			}
		}

		return new SeedResult(pathString, rows.size(), plantsCreated, diseasesInserted, diseasesSkipped, errors);
	}

	private RowOutcome processRow(PlantDiseaseSeedRow row) {
		String plantName = trim(row.getPlantName());
		String diseaseName = trim(row.getDiseaseName());

		if (!StringUtils.hasText(plantName)) {
			throw new IllegalArgumentException("plant_name is required");
		}
		if (!StringUtils.hasText(diseaseName)) {
			throw new IllegalArgumentException("disease_name is required");
		}

		int plantsCreated = 0;
		Plant plant = plantRepository.findByNameIgnoreCase(plantName).orElse(null);
		if (plant == null) {
			plant = plantRepository.save(new Plant(plantName));
			plantsCreated = 1;
		}

		if (diseaseRepository.existsByPlantIdAndDiseaseNameIgnoreCase(plant.getId(), diseaseName)) {
			return new RowOutcome(plantsCreated, 0, 1);
		}

		Disease disease = new Disease(
				plant,
				diseaseName,
				trimToNull(row.getDescription()),
				trimToNull(row.getSymptoms()),
				trimToNull(row.getCauses()),
				trimToNull(row.getSolution()));
		diseaseRepository.save(disease);

		return new RowOutcome(plantsCreated, 1, 0);
	}

	private void validateHeader(Path csvPath) throws IOException {
		String firstLine;
		try (var lines = Files.lines(csvPath)) {
			firstLine = lines.findFirst().orElse("");
		}
		if (!StringUtils.hasText(firstLine)) {
			throw new IOException("CSV is empty");
		}

		List<String> headers = splitCsvLine(firstLine);
		if (!headers.equals(EXPECTED_HEADERS)) {
			throw new IOException("Expected header: " + String.join(", ", EXPECTED_HEADERS) + " but got: "
					+ String.join(", ", headers));
		}
	}

	private List<PlantDiseaseSeedRow> parseRows(Path csvPath) throws IOException {
		try (Reader reader = Files.newBufferedReader(csvPath)) {
			CsvToBean<PlantDiseaseSeedRow> parser = new CsvToBeanBuilder<PlantDiseaseSeedRow>(reader)
					.withType(PlantDiseaseSeedRow.class)
					.withIgnoreLeadingWhiteSpace(true)
					.withIgnoreEmptyLine(true)
					.build();

			List<PlantDiseaseSeedRow> rows = new ArrayList<>();
			for (PlantDiseaseSeedRow row : parser) {
				if (!StringUtils.hasText(trim(row.getPlantName())) && !StringUtils.hasText(trim(row.getDiseaseName()))) {
					continue;
				}
				rows.add(row);
			}
			return rows;
		}
	}

	private static String trim(String value) {
		return value != null ? value.trim() : "";
	}

	private static String trimToNull(String value) {
		String trimmed = trim(value);
		return StringUtils.hasText(trimmed) ? trimmed : null;
	}

	/**
	 * Minimal RFC 4180-style split for the header line only (no embedded newlines).
	 */
	private static List<String> splitCsvLine(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == ',' && !inQuotes) {
				fields.add(current.toString().trim());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		fields.add(current.toString().trim());
		return fields;
	}

	private record RowOutcome(int plantsCreated, int diseasesInserted, int diseasesSkipped) {
	}
}
