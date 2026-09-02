package com.plantdoctor.service;

/**
 * Classifies why vision analysis could not be obtained.
 * Distinct from incomplete vision (partial response) and from healthy assessment.
 */
public enum VisionFailureKind {
	SERVICE_UNAVAILABLE,
	RATE_LIMITED,
	TIMEOUT,
	NETWORK,
	UNKNOWN
}
