package com.euc.grantfitassessment;

import com.euc.core.EucDefinition;
import com.euc.core.EucLoader;
import com.euc.core.ExecutionFilterRegistry;
import com.euc.core.PipelineBuilder;
import com.euc.core.PipelineContext;
import com.euc.grantfitassessment.pipeline.AlignmentReasoningFilter;
import com.euc.grantfitassessment.pipeline.EligibilityRuleFilter;
import com.euc.grantfitassessment.pipeline.GeographyRuleFilter;
import com.euc.grantfitassessment.pipeline.GrantFitContextKeys;
import com.euc.grantfitassessment.pipeline.RequiredInfoRuleFilter;

import java.util.Collections;
import java.util.List;

/**
 * End-to-end Grant Fit Assessment application.
 *
 * Execution is driven mechanically by PipelineBuilder from the EUC's
 * executionPipeline: each stage's `filter` key ("eligibility", "geography",
 * "requiredInfo", "alignmentReasoning") is resolved through an
 * ExecutionFilterRegistry built in the constructor below, rather than
 * hand-wired per use case. Both this class and GrantFitEvaluator load the
 * EUC through the same EucLoader — see docs/proposal.md Section 3 for why
 * that matters.
 */
public class GrantFitApplication {

    private final PipelineBuilder pipelineBuilder;

    public GrantFitApplication(EucDefinition euc, FitReasoner fitReasoner) {
        ExecutionFilterRegistry registry = new ExecutionFilterRegistry()
                .register("eligibility", new EligibilityRuleFilter())
                .register("geography", new GeographyRuleFilter())
                .register("requiredInfo", new RequiredInfoRuleFilter())
                .register("alignmentReasoning", new AlignmentReasoningFilter(fitReasoner, euc));

        this.pipelineBuilder = new PipelineBuilder(euc, registry);
    }

    @SuppressWarnings("unchecked")
    public AssessmentResult assess(Organization org, GrantOpportunity grant) {
        PipelineContext context = new PipelineContext();
        context.put(GrantFitContextKeys.ORGANIZATION, org);
        context.put(GrantFitContextKeys.GRANT, grant);

        pipelineBuilder.run(context);

        boolean eligible = context.get(GrantFitContextKeys.ELIGIBLE, Boolean.class);
        if (!eligible) {
            // Per docs/proposal.md Section 5: a mandatory eligibility failure
            // short-circuits fit reasoning entirely — strong alignment cannot
            // overcome it. The pipeline halts before ALIGNMENT-001 runs (see
            // its onFailure: "halt" in the EUC), so fit fields below are the
            // short-circuit default rather than context reads.
            List<String> failedRules = context.get(GrantFitContextKeys.FAILED_ELIGIBILITY_RULES, List.class);
            return new AssessmentResult(
                    false,
                    failedRules,
                    "POOR_FIT",
                    "Organization does not meet mandatory eligibility requirements.",
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        return new AssessmentResult(
                true,
                Collections.emptyList(),
                context.get(GrantFitContextKeys.FIT_CLASSIFICATION, String.class),
                context.get(GrantFitContextKeys.EXPLANATION, String.class),
                context.get(GrantFitContextKeys.SUPPORTING_EVIDENCE, List.class),
                context.get(GrantFitContextKeys.IDENTIFIED_UNCERTAINTY, List.class)
        );
    }

    public static void main(String[] args) {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();
        System.out.println("Loaded EUC: " + euc.getId() + " — goal: " + euc.getGoal());

        String modelName = System.getenv().getOrDefault("LLM_MODEL", "claude-sonnet-4-6");
        GrantFitApplication app = new GrantFitApplication(euc, new LlmFitReasoner(modelName));

        // Sample run — replace with real input, or use EvaluationRunner for batch runs
        // against eval/grant-fit-assessment/dataset/test-cases.json.
        Organization org = new Organization(
                "Riverside Youth Coalition",
                "Providing after-school STEM programs to underserved youth.",
                List.of("STEM tutoring", "College readiness workshops"),
                "Pacific Northwest",
                true
        );

        GrantOpportunity grant = new GrantOpportunity(
                "Evergreen Community Foundation",
                "Youth Education Grant",
                List.of("STEM education", "Underserved communities"),
                List.of("Registered 501(c)(3)", "Operating in Pacific Northwest"),
                List.of("Pacific Northwest"),
                true
        );

        AssessmentResult result = app.assess(org, grant);
        System.out.println(result);
    }
}
