package com.plantdoctor.service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Detects transient vision-provider failures suitable for retry / fallback.
 */
public final class VisionFailureSupport {

	private static final Set<Integer> TRANSIENT_HTTP_STATUSES = Set.of(
			HttpStatus.TOO_MANY_REQUESTS.value(),
			HttpStatus.BAD_GATEWAY.value(),
			HttpStatus.SERVICE_UNAVAILABLE.value(),
			HttpStatus.GATEWAY_TIMEOUT.value(),
			HttpStatus.REQUEST_TIMEOUT.value());

	private VisionFailureSupport() {
	}

	public static boolean isTransientFailure(Throwable throwable) {
		return classify(throwable) != null;
	}

	public static VisionFailureKind classify(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			VisionFailureKind kind = classifySingle(current);
			if (kind != null) {
				return kind;
			}
			current = current.getCause();
		}
		return null;
	}

	public static VisionUnavailableException toUnavailable(String provider, Throwable throwable) {
		VisionFailureKind kind = classify(throwable);
		if (kind == null) {
			return null;
		}
		String message = provider + " vision failed: " + summarize(throwable);
		return new VisionUnavailableException(provider, kind, message, throwable);
	}

	public static long backoffMillis(int attempt) {
		return 400L * attempt;
	}

	/**
	 * Retry the same vision provider only for timeout/network, and only if leftover budget can pay for another HTTP call.
	 * 503/429 mean the provider is unavailable now — fail over instead of waiting.
	 */
	public static boolean shouldRetrySameProvider(VisionFailureKind kind, int remainingBudgetMs) {
		if (kind == null) {
			return false;
		}
		if (kind == VisionFailureKind.SERVICE_UNAVAILABLE || kind == VisionFailureKind.RATE_LIMITED) {
			return false;
		}
		if (remainingBudgetMs < DiagnosisLatencyBudget.MIN_USEFUL_CALL_MS) {
			return false;
		}
		return kind == VisionFailureKind.TIMEOUT || kind == VisionFailureKind.NETWORK;
	}

	private static VisionFailureKind classifySingle(Throwable throwable) {
		if (throwable instanceof VisionUnavailableException unavailable) {
			return unavailable.kind();
		}
		if (throwable instanceof HttpStatusCodeException http) {
			int status = http.getStatusCode().value();
			if (!TRANSIENT_HTTP_STATUSES.contains(status)) {
				return null;
			}
			if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
				return VisionFailureKind.RATE_LIMITED;
			}
			if (status == HttpStatus.REQUEST_TIMEOUT.value() || status == HttpStatus.GATEWAY_TIMEOUT.value()) {
				return VisionFailureKind.TIMEOUT;
			}
			return VisionFailureKind.SERVICE_UNAVAILABLE;
		}
		if (throwable instanceof ResourceAccessException) {
			if (hasCause(throwable, SocketTimeoutException.class)) {
				return VisionFailureKind.TIMEOUT;
			}
			if (hasCause(throwable, ConnectException.class) || hasCause(throwable, IOException.class)) {
				return VisionFailureKind.NETWORK;
			}
			return VisionFailureKind.NETWORK;
		}
		if (throwable instanceof SocketTimeoutException) {
			return VisionFailureKind.TIMEOUT;
		}
		if (throwable instanceof ConnectException || throwable instanceof IOException) {
			return VisionFailureKind.NETWORK;
		}
		String message = throwable.getMessage();
		if (message != null) {
			String lower = message.toLowerCase(Locale.ROOT);
			if (lower.contains("timed out") || lower.contains("timeout")) {
				return VisionFailureKind.TIMEOUT;
			}
			if (lower.contains("high demand") || lower.contains("503") || lower.contains("service unavailable")) {
				return VisionFailureKind.SERVICE_UNAVAILABLE;
			}
			if (lower.contains("429") || lower.contains("rate limit")) {
				return VisionFailureKind.RATE_LIMITED;
			}
			if (lower.contains("connection") || lower.contains("network")) {
				return VisionFailureKind.NETWORK;
			}
		}
		return null;
	}

	private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
		Throwable current = throwable;
		while (current != null) {
			if (type.isInstance(current)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private static String summarize(Throwable throwable) {
		if (throwable instanceof HttpStatusCodeException http) {
			return http.getStatusCode() + " " + nullToEmpty(http.getStatusText());
		}
		return nullToEmpty(throwable.getMessage());
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
