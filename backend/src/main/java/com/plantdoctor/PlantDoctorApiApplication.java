package com.plantdoctor;

import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.plantdoctor.config.EnvFileLoader;
import com.plantdoctor.config.GroqProperties;
import com.plantdoctor.config.NvidiaProperties;
import com.plantdoctor.config.OpenAiProperties;
import com.plantdoctor.config.SeedProperties;

@SpringBootApplication
@EnableConfigurationProperties({SeedProperties.class, NvidiaProperties.class, OpenAiProperties.class,
		GroqProperties.class})
public class PlantDoctorApiApplication {

	public static void main(String[] args) {
		Path cwd = Path.of(System.getProperty("user.dir", "."));
		EnvFileLoader.loadFirstExisting(cwd.resolve(".env"), cwd.resolve("backend").resolve(".env"));
		SpringApplication.run(PlantDoctorApiApplication.class, args);
	}

}
