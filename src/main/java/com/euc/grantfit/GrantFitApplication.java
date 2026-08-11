package com.euc.grantfit;

import com.euc.core.EucDefinition;
import com.euc.core.EucLoader;

import java.util.Collections;
import java.util.List;

/**
 * End-to-end Grant Fit Assessment application.
 *
 * Both this class and GrantFitEvaluator load the EUC through the same
 * EucLoader — see docs/proposal.md Section 3 for why that matters.
 */
public class GrantFitApplication {

    private final EucDefinition euc;
    private final EligibilityChecker eligibilityChecker;
    private final FitReasoner fitReasoner;

    public GrantFitApplication(EucDefinition euc, EligibilityChecker eligibilityChecker, FitReasoner fitReasoner) {
        this.euc = euc;
        this.eligibilityChecker = eligibilityChecker;
        this.fitReasoner = fitReasoner;
    }

    public AssessmentResult assess(Organization org, GrantOpportunity grant) {
        List<String> failedRules = eligibilityChecker.checkEligibility(org, grant);

        if (!failedRules.isEmpty()) {
            // Per docs/proposal.md Section 5: a mandatory eligibility failure
            // short-circuits fit reasoning entirely — strong alignment cannot
            // overcome it.
            return new AssessmentResult(
                    false,
                    failedRules,
                    "POOR_FIT",
                    "Organization does not meet mandatory eligibility requirements.",
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        FitReasoner.FitReasoning reasoning = fitReasoner.assessFit(org, grant, euc);

        return new AssessmentResult(
                true,
                Collections.emptyList(),
                reasoning.fitClassification(),
                reasoning.explanation(),
                reasoning.supportingEvidence(),
                reasoning.identifiedUncertainty()
        );
    }

    public static void main(String[] args) {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();
        System.out.println("Loaded EUC: " + euc.getId() + " — goal: " + euc.getGoal());

        GrantFitApplication app = new GrantFitApplication(
                euc,
                new EligibilityChecker(),
                new LlmFitReasoner("configure-model-here")
        );

        // Sample run — replace with real input or wire to eval/dataset for batch runs.
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
