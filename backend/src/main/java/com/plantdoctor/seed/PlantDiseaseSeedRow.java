package com.plantdoctor.seed;

import com.opencsv.bean.CsvBindByName;

/**
 * One row from {@code plant-disease-seed-template.csv}.
 */
public class PlantDiseaseSeedRow {

	@CsvBindByName(column = "plant_name", required = true)
	private String plantName;

	@CsvBindByName(column = "disease_name", required = true)
	private String diseaseName;

	@CsvBindByName(column = "description")
	private String description;

	@CsvBindByName(column = "symptoms")
	private String symptoms;

	@CsvBindByName(column = "causes")
	private String causes;

	@CsvBindByName(column = "solution")
	private String solution;

	public String getPlantName() {
		return plantName;
	}

	public void setPlantName(String plantName) {
		this.plantName = plantName;
	}

	public String getDiseaseName() {
		return diseaseName;
	}

	public void setDiseaseName(String diseaseName) {
		this.diseaseName = diseaseName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSymptoms() {
		return symptoms;
	}

	public void setSymptoms(String symptoms) {
		this.symptoms = symptoms;
	}

	public String getCauses() {
		return causes;
	}

	public void setCauses(String causes) {
		this.causes = causes;
	}

	public String getSolution() {
		return solution;
	}

	public void setSolution(String solution) {
		this.solution = solution;
	}

}
