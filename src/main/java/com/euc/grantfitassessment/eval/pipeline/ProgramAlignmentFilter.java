package com.euc.grantfitassessment.eval.pipeline;

import com.euc.core.EvaluationFilter;
import com.euc.core.EvaluationStage;
import com.euc.core.PipelineContext;
import com.euc.grantfitassessment.pipeline.GrantFitContextKeys;

/**
 * Scores EUC evaluation stage "programAlignment": the reasoned fit
 * classification must match ground truth. An eligibility failure
 * short-circuits fit reasoning entirely (docs/proposal.md Section 5), so
 * there is nothing to score here when the case is ineligible — it passes
 * vacuously rather than penalizing a case this criterion doesn't apply to.
 */
public class ProgramAlignmentFilter implements EvaluationFilter {

    @Override
    public Verdict evaluate(PipelineContext context, EvaluationStage stage) {
        boolean actualEligible = context.get(GrantFitContextKeys.ELIGIBLE, Boolean.class);
        if (!actualEligible) {
            return Verdict.PASSED;
        }

        String actualFit = context.get(GrantFitContextKeys.FIT_CLASSIFICATION, String.class);
        String expectedFit = context.get(GrantFitEvalContextKeys.EXPECTED_FIT_CLASSIFICATION, String.class);
        return expectedFit.equals(actualFit) ? Verdict.PASSED : Verdict.FAILED;
    }
}
