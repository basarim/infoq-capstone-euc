package com.euc.core;

/**
 * A single filter in the evaluation pipeline, mirroring ExecutionFilter's
 * shape so the same schema-driven pattern covers both halves of the EUC
 * (docs/proposal.md Section 3). Implementations are looked up by
 * EvaluationPipelineBuilder via the `filter` key each EvaluationStage
 * declares.
 */
public interface EvaluationFilter {

    enum Verdict {
        PASSED,
        FAILED
    }

    /**
     * Scores this criterion against the shared context, reading whatever
     * fields it declares in `reads` (typically fields an execution stage
     * wrote, plus ground-truth fields the caller seeds separately — see
     * EvaluationPipelineBuilder).
     */
    Verdict evaluate(PipelineContext context, EvaluationStage stage);
}
