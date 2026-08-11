package com.euc.grantfit;

import java.util.List;

/**
 * The reasoned result of a Grant Fit Assessment.
 *
 * fitClassification must be one of the EUC's expectedOutcomes
 * (STRONG_FIT / POSSIBLE_FIT / POOR_FIT). eligible is decided purely by
 * deterministic rules and short-circuits fit reasoning when false, per
 * docs/proposal.md Section 5: "strong program alignment cannot overcome
 * a mandatory eligibility requirement that the organization does not meet."
 */
public record AssessmentResult(
        boolean eligible,
        List<String> failedEligibilityRules,
        String fitClassification,
        String explanation,
        List<String> supportingEvidence,
        List<String> identifiedUncertainty
) {}
