package com.plantdoctor.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantdoctor.entity.Plant;

public interface PlantRepository extends JpaRepository<Plant, Integer> {

	Optional<Plant> findByNameIgnoreCase(String name);

}
