package com.euc.core;

/**
 * The implementation that scores one EvaluationCriterion.
 *
 * Registered against the id of the criterion it measures (see
 * EvaluationFilterRegistry), so an evaluator declares which criterion it
 * covers — and, through that criterion's `tracesTo`, which business
 * requirements its verdict speaks to.
 *
 * In the full design this is where an existing evaluation framework plugs
 * in: a criterion maps to a deterministic assertion, a standard metric, or
 * an LLM-as-a-judge rubric (docs/proposal.md Section 5). The prototype
 * implements the assertions directly.
 */
public interface EvaluationFilter {

    enum Verdict {
        PASSED,
        FAILED
    }

    /**
     * Scores this criterion against the shared context, reading the fields
     * it declares in `reads` — typically fields an execution requirement
     * wrote, plus whatever ground truth the caller seeds separately.
     */
    Verdict evaluate(PipelineContext context, EvaluationCriterion criterion);
}
