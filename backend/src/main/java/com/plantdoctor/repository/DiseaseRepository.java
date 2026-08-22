package com.plantdoctor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantdoctor.entity.Disease;

public interface DiseaseRepository extends JpaRepository<Disease, Integer> {

	boolean existsByPlantIdAndDiseaseNameIgnoreCase(Integer plantId, String diseaseName);

}
