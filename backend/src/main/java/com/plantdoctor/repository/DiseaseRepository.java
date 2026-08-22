package com.plantdoctor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.plantdoctor.entity.Disease;

public interface DiseaseRepository extends JpaRepository<Disease, Integer> {

	boolean existsByPlantIdAndDiseaseNameIgnoreCase(Integer plantId, String diseaseName);

	@Query("SELECT d FROM Disease d JOIN FETCH d.plant")
	List<Disease> findAllWithPlant();

}
