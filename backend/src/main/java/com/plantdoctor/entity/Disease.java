package com.plantdoctor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "diseases")
public class Disease {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plant_id")
	private Plant plant;

	@Column(name = "disease_name", nullable = false)
	private String diseaseName;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String symptoms;

	@Column(columnDefinition = "TEXT")
	private String causes;

	@Column(columnDefinition = "TEXT")
	private String solution;

	protected Disease() {
	}

	public Disease(Plant plant, String diseaseName, String description, String symptoms, String causes,
			String solution) {
		this.plant = plant;
		this.diseaseName = diseaseName;
		this.description = description;
		this.symptoms = symptoms;
		this.causes = causes;
		this.solution = solution;
	}

	public Integer getId() {
		return id;
	}

	public Plant getPlant() {
		return plant;
	}

	public String getDiseaseName() {
		return diseaseName;
	}

	public String getDescription() {
		return description;
	}

	public String getSymptoms() {
		return symptoms;
	}

	public String getCauses() {
		return causes;
	}

	public String getSolution() {
		return solution;
	}

}
