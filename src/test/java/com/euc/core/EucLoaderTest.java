package com.euc.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sanity check that the EUC resource loads and parses correctly — the
 * "does the single source of truth even load" test that everything else
 * depends on.
 */
class EucLoaderTest {

    @Test
    void loadsGrantFitAssessmentEuc() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        assertEquals("grant-fit-assessment", euc.getId());
        assertEquals(3, euc.getExpectedOutcomes().size());
        assertTrue(euc.getExpectedOutcomes().contains("STRONG_FIT"));
        assertTrue(euc.getEvaluation().contains("eligibilityCorrectness"));
    }

    @Test
    void separatesDeterministicAndReasonedRules() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        assertTrue(euc.deterministicRules().size() >= 3, "expected multiple deterministic rules");
        assertEquals(1, euc.reasonedRules().size(), "expected exactly one reasoned rule (ALIGNMENT-001)");
    }
}
