package com.euc.grantfitassessment.eval.pipeline;

import com.euc.core.EvaluationFilter;
import com.euc.core.EvaluationCriterion;
import com.euc.core.PipelineContext;
import com.euc.grantfitassessment.pipeline.GrantFitContextKeys;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores EUC evaluation stage "evidenceGrounding": every expected evidence
 * keyword (established independently as ground truth) must appear in the
 * reasoner's cited supportingEvidence. Same vacuous-pass rule as
 * ProgramAlignmentFilter for ineligible cases — nothing was reasoned about,
 * so there is nothing to check evidence for.
 */
public class EvidenceGroundingFilter implements EvaluationFilter {

    @Override
    @SuppressWarnings("unchecked")
    public Verdict evaluate(PipelineContext context, EvaluationCriterion stage) {
        boolean actualEligible = context.get(GrantFitContextKeys.ELIGIBLE, Boolean.class);
        if (!actualEligible) {
            context.put(GrantFitEvalContextKeys.MISSING_EVIDENCE_KEYWORDS, List.of());
            return Verdict.PASSED;
        }

        List<String> evidence = context.get(GrantFitContextKeys.SUPPORTING_EVIDENCE, List.class);
        List<String> expectedKeywords = context.get(GrantFitEvalContextKeys.EXPECTED_EVIDENCE_KEYWORDS, List.class);

        List<String> missing = new ArrayList<>();
        for (String keyword : expectedKeywords) {
            boolean found = evidence.stream().anyMatch(e -> e.toLowerCase().contains(keyword.toLowerCase()));
            if (!found) {
                missing.add(keyword);
            }
        }

        context.put(GrantFitEvalContextKeys.MISSING_EVIDENCE_KEYWORDS, missing);
        return missing.isEmpty() ? Verdict.PASSED : Verdict.FAILED;
    }
}
