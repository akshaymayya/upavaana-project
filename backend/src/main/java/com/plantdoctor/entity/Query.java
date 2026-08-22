package com.plantdoctor.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "queries")
public class Query {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "result_json", columnDefinition = "TEXT")
	private String resultJson;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false, insertable = false)
	private LocalDateTime createdAt;

	protected Query() {
	}

	public Query(String imageUrl, String resultJson) {
		this.imageUrl = imageUrl;
		this.resultJson = resultJson;
	}

	public Integer getId() {
		return id;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getResultJson() {
		return resultJson;
	}

	public void setResultJson(String resultJson) {
		this.resultJson = resultJson;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

}
