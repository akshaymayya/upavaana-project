package com.plantdoctor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.plantdoctor.config.SeedProperties;

@SpringBootApplication
@EnableConfigurationProperties(SeedProperties.class)
public class PlantDoctorApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlantDoctorApiApplication.class, args);
	}

}
