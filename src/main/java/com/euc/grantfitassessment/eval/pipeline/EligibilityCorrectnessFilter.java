package com.euc.grantfitassessment.eval.pipeline;

import com.euc.core.EvaluationFilter;
import com.euc.core.EvaluationStage;
import com.euc.core.PipelineContext;
import com.euc.grantfitassessment.pipeline.GrantFitContextKeys;

/** Scores EUC evaluation stage "eligibilityCorrectness": actual eligibility must match the independently-established ground truth. */
public class EligibilityCorrectnessFilter implements EvaluationFilter {

    @Override
    public Verdict evaluate(PipelineContext context, EvaluationStage stage) {
        boolean actual = context.get(GrantFitContextKeys.ELIGIBLE, Boolean.class);
        boolean expected = context.get(GrantFitEvalContextKeys.EXPECTED_ELIGIBLE, Boolean.class);
        return actual == expected ? Verdict.PASSED : Verdict.FAILED;
    }
}
