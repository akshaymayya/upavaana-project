package com.plantdoctor.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.plantdoctor.service.DiagnosisResult;
import com.plantdoctor.service.DiagnosisService;

@RestController
@RequestMapping("/api")
public class DiagnoseController {

	private final DiagnosisService diagnosisService;

	public DiagnoseController(DiagnosisService diagnosisService) {
		this.diagnosisService = diagnosisService;
	}

	@PostMapping("/diagnose")
	public ResponseEntity<?> diagnose(@RequestParam("image") MultipartFile image) {
		try {
			DiagnosisResult result = diagnosisService.diagnosePlant(image);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", ex.getMessage()));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Failed to diagnose plant: " + ex.getMessage()));
		}
	}

}
