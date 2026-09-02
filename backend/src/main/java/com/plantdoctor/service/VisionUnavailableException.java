package com.plantdoctor.service;

/**
 * Vision analysis could not be obtained (provider outage, rate limit, timeout, network).
 * Must not be treated as evidence of a healthy plant or absence of symptoms.
 */
public class VisionUnavailableException extends RuntimeException {

	private final String provider;
	private final VisionFailureKind kind;

	public VisionUnavailableException(String provider, VisionFailureKind kind, String message, Throwable cause) {
		super(message, cause);
		this.provider = provider;
		this.kind = kind;
	}

	public VisionUnavailableException(String provider, VisionFailureKind kind, String message) {
		this(provider, kind, message, null);
	}

	public String provider() {
		return provider;
	}

	public VisionFailureKind kind() {
		return kind;
	}

	public String userFacingDetail() {
		return switch (kind) {
			case SERVICE_UNAVAILABLE -> "vision service temporarily unavailable (high demand)";
			case RATE_LIMITED -> "vision service rate limit reached";
			case TIMEOUT -> "vision request timed out";
			case NETWORK -> "network error contacting vision service";
			case UNKNOWN -> "vision service error";
		};
	}
}
