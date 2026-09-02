package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class VisionFailureSupportTest {

	@Test
	void classifies503AsTransientServiceUnavailable() {
		HttpServerErrorException ex = HttpServerErrorException.create(
				HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", null, null, null);
		assertEquals(VisionFailureKind.SERVICE_UNAVAILABLE, VisionFailureSupport.classify(ex));
		assertTrue(VisionFailureSupport.isTransientFailure(ex));
	}

	@Test
	void classifies429AsRateLimited() {
		HttpServerErrorException ex = HttpServerErrorException.create(
				HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null);
		assertEquals(VisionFailureKind.RATE_LIMITED, VisionFailureSupport.classify(ex));
	}

	@Test
	void classifiesNetworkTimeout() {
		ResourceAccessException ex = new ResourceAccessException(
				"I/O error on POST request", new java.net.SocketTimeoutException("Read timed out"));
		assertEquals(VisionFailureKind.TIMEOUT, VisionFailureSupport.classify(ex));
	}

	@Test
	void classifiesNetworkConnectionFailure() {
		ResourceAccessException ex = new ResourceAccessException(
				"I/O error on POST request", new java.net.ConnectException("Connection refused"));
		assertEquals(VisionFailureKind.NETWORK, VisionFailureSupport.classify(ex));
	}

	@Test
	void highDemandMessage_classifiedAsServiceUnavailable() {
		RuntimeException ex = new RuntimeException("This model is currently experiencing high demand.");
		assertEquals(VisionFailureKind.SERVICE_UNAVAILABLE, VisionFailureSupport.classify(ex));
	}

	@Test
	void nonTransientClientError_notClassified() {
		HttpServerErrorException ex = HttpServerErrorException.create(
				HttpStatus.BAD_REQUEST, "Bad Request", null, null, null);
		assertNull(VisionFailureSupport.classify(ex));
	}

	@Test
	void toUnavailable_wrapsTransientFailure() {
		HttpServerErrorException ex = HttpServerErrorException.create(
				HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", null, null, null);
		VisionUnavailableException unavailable = VisionFailureSupport.toUnavailable("gemini", ex);
		assertNotNull(unavailable);
		assertEquals(VisionFailureKind.SERVICE_UNAVAILABLE, unavailable.kind());
		assertEquals("gemini", unavailable.provider());
	}

	@Test
	void shouldRetrySameProvider_503DoesNotRetry() {
		assertFalse(VisionFailureSupport.shouldRetrySameProvider(
				VisionFailureKind.SERVICE_UNAVAILABLE, 8_000));
		assertFalse(VisionFailureSupport.shouldRetrySameProvider(
				VisionFailureKind.RATE_LIMITED, 8_000));
	}

	@Test
	void shouldRetrySameProvider_timeoutOnlyIfBudgetRemains() {
		assertTrue(VisionFailureSupport.shouldRetrySameProvider(VisionFailureKind.TIMEOUT, 3_000));
		assertFalse(VisionFailureSupport.shouldRetrySameProvider(
				VisionFailureKind.TIMEOUT, DiagnosisLatencyBudget.MIN_USEFUL_CALL_MS - 1));
	}
}
