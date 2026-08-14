package com.euc.grantfitassessment.pipeline;

import com.euc.core.EucDefinition;
import com.euc.core.EucRule;
import com.euc.core.ExecutionFilter;
import com.euc.core.PipelineContext;
import com.euc.grantfitassessment.FitReasoner;
import com.euc.grantfitassessment.GrantOpportunity;
import com.euc.grantfitassessment.Organization;

/**
 * Runs EUC stage ALIGNMENT-001 (filter key "alignmentReasoning") by
 * delegating to a FitReasoner. This is the filter that gets swapped during
 * the drift experiments (docs/proposal.md Section 7) — a different
 * FitReasoner implementation plugs in here without touching the EUC, the
 * pipeline, or any other filter.
 */
public class AlignmentReasoningFilter implements ExecutionFilter {

    private final FitReasoner reasoner;
    private final EucDefinition euc;

    public AlignmentReasoningFilter(FitReasoner reasoner, EucDefinition euc) {
        this.reasoner = reasoner;
        this.euc = euc;
    }

    @Override
    public Outcome execute(PipelineContext context, EucRule stage) {
        Organization org = context.get(GrantFitContextKeys.ORGANIZATION, Organization.class);
        GrantOpportunity grant = context.get(GrantFitContextKeys.GRANT, GrantOpportunity.class);

        FitReasoner.FitReasoning reasoning = reasoner.assessFit(org, grant, euc);

        context.put(GrantFitContextKeys.FIT_CLASSIFICATION, reasoning.fitClassification());
        context.put(GrantFitContextKeys.EXPLANATION, reasoning.explanation());
        context.put(GrantFitContextKeys.SUPPORTING_EVIDENCE, reasoning.supportingEvidence());
        context.put(GrantFitContextKeys.IDENTIFIED_UNCERTAINTY, reasoning.identifiedUncertainty());

        return Outcome.PASSED;
    }
}
