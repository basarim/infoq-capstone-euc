package com.euc.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void loadedEucSatisfiesPipelineContract() {
        // A pipeline is comprised of one or more filters — assert the real
        // EUC actually meets that contract, not just that validate() exists.
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        assertTrue(euc.getExecutionPipeline().size() >= 1);
        assertTrue(euc.getEvaluationPipeline().size() >= 1);
    }

    @Test
    void emptyExecutionPipelineFailsValidation() {
        EucDefinition euc = new EucDefinition();
        euc.setId("empty-execution-euc");
        euc.setExecutionPipeline(List.of());
        euc.setEvaluationPipeline(List.of(sampleEvaluationStage()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, euc::validate);
        assertTrue(ex.getMessage().contains("executionPipeline"));
    }

    @Test
    void emptyEvaluationPipelineFailsValidation() {
        EucDefinition euc = new EucDefinition();
        euc.setId("empty-evaluation-euc");
        euc.setExecutionPipeline(List.of(sampleExecutionStage()));
        euc.setEvaluationPipeline(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, euc::validate);
        assertTrue(ex.getMessage().contains("evaluationPipeline"));
    }

    @Test
    void stageWithoutFilterKeyFailsValidation() {
        EucRule stageMissingFilter = new EucRule();
        stageMissingFilter.setId("BAD-001");
        stageMissingFilter.setType(EucRule.Type.DETERMINISTIC);
        // filter intentionally left unset

        EucDefinition euc = new EucDefinition();
        euc.setId("missing-filter-euc");
        euc.setExecutionPipeline(List.of(stageMissingFilter));
        euc.setEvaluationPipeline(List.of(sampleEvaluationStage()));

        assertThrows(IllegalStateException.class, euc::validate);
    }

    @Test
    void singleStagePipelineIsValid() {
        // "One or more" means a single filter is a valid pipeline, not just
        // multi-stage ones — this asserts the boundary case explicitly.
        EucDefinition euc = new EucDefinition();
        euc.setId("single-stage-euc");
        euc.setExecutionPipeline(List.of(sampleExecutionStage()));
        euc.setEvaluationPipeline(List.of(sampleEvaluationStage()));

        assertDoesNotThrow(euc::validate);
    }

    private EucRule sampleExecutionStage() {
        EucRule stage = new EucRule();
        stage.setId("SAMPLE-001");
        stage.setFilter("sampleFilter");
        stage.setType(EucRule.Type.DETERMINISTIC);
        return stage;
    }

    private EvaluationStage sampleEvaluationStage() {
        EvaluationStage stage = new EvaluationStage();
        stage.setId("sampleEvaluation");
        stage.setFilter("sampleEvaluationFilter");
        stage.setEvaluates(List.of("SAMPLE-001"));
        return stage;
    }
}
