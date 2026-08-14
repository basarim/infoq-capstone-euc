package com.euc.grantfit.pipeline;

import com.euc.core.EucRule;
import com.euc.core.ExecutionFilter;
import com.euc.core.PipelineContext;
import com.euc.grantfit.GrantOpportunity;
import com.euc.grantfit.Organization;

import java.util.List;

/** Runs EUC stage GEOGRAPHY-001 (filter key "geography"): must operate within the grant's allowed regions. */
public class GeographyRuleFilter implements ExecutionFilter {

    @Override
    public Outcome execute(PipelineContext context, EucRule stage) {
        Organization org = context.get(GrantFitContextKeys.ORGANIZATION, Organization.class);
        GrantOpportunity grant = context.get(GrantFitContextKeys.GRANT, GrantOpportunity.class);

        if (!grant.allowedRegions().isEmpty() && !grant.allowedRegions().contains(org.operatingRegion())) {
            context.put(GrantFitContextKeys.ELIGIBLE, false);
            context.put(GrantFitContextKeys.FAILED_ELIGIBILITY_RULES,
                    List.of(stage.getId() + ": operating region '" + org.operatingRegion()
                            + "' is outside the grant's allowed regions " + grant.allowedRegions()));
            return Outcome.FAILED;
        }

        context.put(GrantFitContextKeys.ELIGIBLE, true);
        return Outcome.PASSED;
    }
}
