package com.plantdoctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DiagnosisLatencyBudgetTest {

	@Test
	void nvidiaFallbackReadIsProviderOwnedNotLeftoverVisionTime() throws Exception {
		DiagnosisLatencyBudget budget = new DiagnosisLatencyBudget(28_000, 1_000, 8_000, 8_000);
		Thread.sleep(50);
		assertTrue(budget.remainingVisionMs() < 8_000);
		assertEquals(8_000, budget.nvidiaFallbackReadTimeoutMs(8_000));
		assertEquals(10_000, budget.nvidiaFallbackReadTimeoutMs(10_000));
		assertTrue(budget.canStartProviderCall());
	}

	@Test
	void groqReadIsCappedBySynthesisMax() {
		DiagnosisLatencyBudget budget = new DiagnosisLatencyBudget(28_000, 16_000, 8_000, 8_000);
		assertEquals(8_000, budget.groqReadTimeoutMs(8_000));
	}

	@Test
	void remainingTotalExhaustedBlocksNewCalls() throws Exception {
		DiagnosisLatencyBudget budget = new DiagnosisLatencyBudget(30, 16_000, 8_000, 8_000);
		Thread.sleep(40);
		assertFalse(budget.canStartProviderCall());
		assertEquals(8_000, budget.nvidiaFallbackReadTimeoutMs(8_000));
	}
}
