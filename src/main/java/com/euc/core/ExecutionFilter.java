package com.euc.core;

/**
 * The implementation that carries out one ExecutionRequirement.
 *
 * Implementations are registered against the id of the requirement they
 * satisfy (see ExecutionFilterRegistry), which is the direction that matters:
 * the code names the business requirement, not the other way round. That is
 * what makes "which requirement does this code satisfy?" answerable, and it
 * keeps the EUC free of any mention of the classes that run it.
 *
 * Pipe-and-filter is one reasonable way to arrange this and is what the
 * prototype uses; it is an implementation choice, not part of the EUC
 * concept (docs/proposal.md, Appendix A).
 */
public interface ExecutionFilter {

    enum Outcome {
        PASSED,
        FAILED
    }

    /**
     * Carries out the requirement against the shared context, reading the
     * fields it declares in `reads` and writing those it declares in
     * `writes`. Returning FAILED only stops the run if the requirement's
     * onFailure policy is HALT — PipelineBuilder makes that call, not the
     * implementation.
     */
    Outcome execute(PipelineContext context, ExecutionRequirement requirement);
}
