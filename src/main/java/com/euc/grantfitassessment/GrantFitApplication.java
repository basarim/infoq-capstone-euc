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
 * The EUC says what must happen; the registry below says which class does
 * each part. Every entry names an execution requirement id declared in
 * grant-fit-assessment.json, so the code points at the business requirement
 * it satisfies rather than the EUC pointing at code.
 *
 * This class and GrantFitEvaluator load the EUC through the same EucLoader,
 * which is what makes them two consumers of one definition rather than two
 * interpretations of it.
 */
public class GrantFitApplication {

    private final PipelineBuilder pipelineBuilder;

    public GrantFitApplication(EucDefinition euc, FitReasoner fitReasoner) {
        ExecutionFilterRegistry registry = new ExecutionFilterRegistry()
                .register("ELIGIBILITY-001", new EligibilityRuleFilter())
                .register("GEOGRAPHY-001", new GeographyRuleFilter())
                .register("INFO-001", new RequiredInfoRuleFilter())
                .register("ALIGNMENT-001", new AlignmentReasoningFilter(fitReasoner, euc));

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
            // RULE-ELIGIBILITY-PRECEDES-FIT: strong alignment cannot overcome a
            // failed mandatory requirement. The run halts before ALIGNMENT-001
            // (its onFailure is "halt" in the EUC), so the fit fields below are
            // the short-circuit default rather than context reads — the model
            // was never asked.
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
