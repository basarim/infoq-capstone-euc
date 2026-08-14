package com.euc.grantfitassessment;

import com.euc.core.EucDefinition;

import java.util.List;

/**
 * Deterministic FitReasoner test double — no network call, no API key.
 * Lets tests exercise the full EUC-driven pipeline (GrantFitApplication ->
 * PipelineBuilder -> filters) without depending on a live model, and lets
 * a test assert the reasoner was never invoked when eligibility should
 * have halted the pipeline first.
 */
public class FakeFitReasoner implements FitReasoner {

    private final FitReasoning canned;
    private boolean invoked = false;

    public FakeFitReasoner(FitReasoning canned) {
        this.canned = canned;
    }

    public static FakeFitReasoner returning(String fitClassification, String explanation,
                                             List<String> supportingEvidence, List<String> identifiedUncertainty) {
        return new FakeFitReasoner(new FitReasoning(fitClassification, explanation, supportingEvidence, identifiedUncertainty));
    }

    public static FakeFitReasoner thatMustNotBeInvoked() {
        return new FakeFitReasoner(null);
    }

    @Override
    public FitReasoning assessFit(Organization org, GrantOpportunity grant, EucDefinition euc) {
        invoked = true;
        if (canned == null) {
            throw new AssertionError(
                    "FitReasoner.assessFit() was called but the pipeline should have halted "
                            + "before ALIGNMENT-001 ran (an earlier eligibility stage should have failed)");
        }
        return canned;
    }

    public boolean wasInvoked() {
        return invoked;
    }
}
