package com.euc.grantfit;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic eligibility rules (EUC rules ELIGIBILITY-001, GEOGRAPHY-001,
 * INFO-001). No model call — these either pass or fail, and per the
 * project's falsifiable claims (docs/proposal.md Section 4), results here
 * must remain stable regardless of which prompt or model the reasoning
 * layer uses.
 */
public class EligibilityChecker {

    public List<String> checkEligibility(Organization org, GrantOpportunity grant) {
        List<String> failedRules = new ArrayList<>();

        // ELIGIBILITY-001: must be a registered nonprofit, if required
        if (grant.requiresRegisteredNonprofit() && !org.isRegisteredNonprofit()) {
            failedRules.add("ELIGIBILITY-001: organization is not a registered nonprofit");
        }

        // GEOGRAPHY-001: must operate within the grant's allowed regions
        if (!grant.allowedRegions().isEmpty()
                && !grant.allowedRegions().contains(org.operatingRegion())) {
            failedRules.add("GEOGRAPHY-001: operating region '" + org.operatingRegion()
                    + "' is outside the grant's allowed regions " + grant.allowedRegions());
        }

        // INFO-001: required application information must be present
        if (org.missionStatement() == null || org.missionStatement().isBlank()) {
            failedRules.add("INFO-001: organization mission statement is missing");
        }
        if (org.programs() == null || org.programs().isEmpty()) {
            failedRules.add("INFO-001: organization programs are missing");
        }

        return failedRules;
    }
}
