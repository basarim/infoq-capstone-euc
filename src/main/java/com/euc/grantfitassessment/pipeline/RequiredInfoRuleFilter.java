package com.euc.grantfitassessment.pipeline;

import com.euc.core.EucRule;
import com.euc.core.ExecutionFilter;
import com.euc.core.PipelineContext;
import com.euc.grantfitassessment.Organization;

import java.util.ArrayList;
import java.util.List;

/** Runs EUC stage INFO-001 (filter key "requiredInfo"): required application information must be present. */
public class RequiredInfoRuleFilter implements ExecutionFilter {

    @Override
    public Outcome execute(PipelineContext context, EucRule stage) {
        Organization org = context.get(GrantFitContextKeys.ORGANIZATION, Organization.class);

        List<String> failures = new ArrayList<>();
        if (org.missionStatement() == null || org.missionStatement().isBlank()) {
            failures.add(stage.getId() + ": organization mission statement is missing");
        }
        if (org.programs() == null || org.programs().isEmpty()) {
            failures.add(stage.getId() + ": organization programs are missing");
        }

        if (!failures.isEmpty()) {
            context.put(GrantFitContextKeys.ELIGIBLE, false);
            context.put(GrantFitContextKeys.FAILED_ELIGIBILITY_RULES, failures);
            return Outcome.FAILED;
        }

        context.put(GrantFitContextKeys.ELIGIBLE, true);
        return Outcome.PASSED;
    }
}
