package com.euc.grantfit;

import java.util.List;

/** A grant opportunity — the second of the two inputs to a Grant Fit Assessment. */
public record GrantOpportunity(
        String funderName,
        String grantName,
        List<String> fundingPriorities,
        List<String> eligibilityRequirements,
        List<String> allowedRegions,
        boolean requiresRegisteredNonprofit
) {}
