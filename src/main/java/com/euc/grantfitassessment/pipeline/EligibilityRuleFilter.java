package com.euc.grantfitassessment.pipeline;

import com.euc.core.ExecutionRequirement;
import com.euc.core.ExecutionFilter;
import com.euc.core.PipelineContext;
import com.euc.grantfitassessment.GrantOpportunity;
import com.euc.grantfitassessment.Organization;

import java.util.List;

/** Runs EUC stage ELIGIBILITY-001 (filter key "eligibility"): must be a registered nonprofit, if the grant requires it. */
public class EligibilityRuleFilter implements ExecutionFilter {

    @Override
    public Outcome execute(PipelineContext context, ExecutionRequirement stage) {
        Organization org = context.get(GrantFitContextKeys.ORGANIZATION, Organization.class);
        GrantOpportunity grant = context.get(GrantFitContextKeys.GRANT, GrantOpportunity.class);

        if (grant.requiresRegisteredNonprofit() && !org.isRegisteredNonprofit()) {
            context.put(GrantFitContextKeys.ELIGIBLE, false);
            context.put(GrantFitContextKeys.FAILED_ELIGIBILITY_RULES,
                    List.of(stage.getId() + ": organization is not a registered nonprofit"));
            return Outcome.FAILED;
        }

        context.put(GrantFitContextKeys.ELIGIBLE, true);
        return Outcome.PASSED;
    }
}
