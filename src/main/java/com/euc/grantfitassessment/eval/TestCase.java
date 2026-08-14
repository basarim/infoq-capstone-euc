package com.euc.grantfitassessment.eval;

import com.euc.grantfitassessment.GrantOpportunity;
import com.euc.grantfitassessment.Organization;

import java.util.List;

/**
 * A single eval dataset entry: an (Organization, GrantOpportunity) pair
 * with an independently-established ground truth, per docs/proposal.md
 * Section 6, step 2 ("Establish ground truth ... independently of the
 * application").
 */
public record TestCase(
        String caseId,
        Organization organization,
        GrantOpportunity grant,
        boolean expectedEligible,
        String expectedFitClassification,
        String groundTruthRationale,
        List<String> expectedEvidenceKeywords
) {}
