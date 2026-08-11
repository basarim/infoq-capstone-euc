package com.euc.grantfit;

import com.euc.core.EucDefinition;

/**
 * Handles the EUC's "reasoned" rules (ALIGNMENT-001): mission/program
 * alignment, evidence interpretation, and explanation generation.
 *
 * This is deliberately an interface. Per docs/proposal.md Section 4, the
 * project's central claim is that swapping the implementation behind this
 * interface (a different prompt, a different model) should be detectable
 * by evaluation against the unchanged EUC — the interface boundary is
 * where that swap happens without touching EucDefinition or the
 * deterministic layer.
 */
public interface FitReasoner {

    /**
     * Produces a fit classification (must be one of euc.getExpectedOutcomes()),
     * an explanation, supporting evidence drawn from the organization's
     * profile, and any uncertainty the reasoner could not resolve.
     */
    FitReasoning assessFit(Organization org, GrantOpportunity grant, EucDefinition euc);

    record FitReasoning(
            String fitClassification,
            String explanation,
            java.util.List<String> supportingEvidence,
            java.util.List<String> identifiedUncertainty
    ) {}
}
