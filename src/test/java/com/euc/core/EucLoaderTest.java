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
        assertTrue(euc.getEvaluationPipeline().stream()
                .anyMatch(stage -> stage.getId().equals("eligibilityCorrectness")));
    }

    @Test
    void separatesDeterministicAndReasonedStages() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        assertTrue(euc.deterministicStages().size() >= 3, "expected multiple deterministic stages");
        assertEquals(1, euc.reasonedStages().size(), "expected exactly one reasoned stage (ALIGNMENT-001)");
    }

    @Test
    void deterministicStagesHaltOnFailure() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        for (EucRule stage : euc.deterministicStages()) {
            assertEquals(EucRule.OnFailure.HALT, stage.getOnFailure(),
                    stage.getId() + " should halt the pipeline on failure");
        }
    }

    @Test
    void evaluationStagesReferenceValidExecutionStageIds() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        for (EvaluationStage stage : euc.getEvaluationPipeline()) {
            for (String executionStageId : stage.getEvaluates()) {
                // findStage throws if the id doesn't exist — asserting no exception
                // confirms every evaluation stage points at a real execution stage.
                euc.findStage(executionStageId);
            }
        }
    }
}
