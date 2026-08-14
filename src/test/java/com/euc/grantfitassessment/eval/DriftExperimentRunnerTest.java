package com.euc.grantfitassessment.eval;

import com.euc.core.EucDefinition;
import com.euc.core.EucLoader;
import com.euc.grantfitassessment.FakeFitReasoner;
import com.euc.grantfitassessment.FitReasoner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the drift-experiment mechanics (docs/proposal.md Section 7) work
 * offline, with fake FitReasoners standing in for real model/prompt
 * variants — no LLM_API_KEY needed. A live run (DriftExperimentMain) still
 * needs a real key; this test only verifies the metric computation itself
 * is correct once outcomes come back.
 */
class DriftExperimentRunnerTest {

    private final EucDefinition euc = EucLoader.loadGrantFitAssessment();

    private TestCase eligibleGroundTruth(String caseId, String expectedFit, List<String> expectedKeywords) {
        return new TestCase(
                caseId,
                new com.euc.grantfitassessment.Organization("Org", "Mission", List.of("Program"), "PNW", true),
                new com.euc.grantfitassessment.GrantOpportunity("Funder", "Grant", List.of("Priority"), List.of(), List.of("PNW"), true),
                true,
                expectedFit,
                "rationale",
                expectedKeywords
        );
    }

    private FitReasoner reasonerAlwaysReturning(String fitClassification, List<String> evidence) {
        return FakeFitReasoner.returning(fitClassification, "explanation", evidence, List.of());
    }

    @Test
    void behaviorAlteringVariantThatDisagreesWithGroundTruthIsFlaggedAsDrift() {
        List<TestCase> dataset = List.of(eligibleGroundTruth("case-1", "STRONG_FIT", List.of("STEM")));

        FitReasonerVariant baseline = new FitReasonerVariant(
                "baseline", reasonerAlwaysReturning("STRONG_FIT", List.of("STEM alignment")), false);
        FitReasonerVariant driftedVariant = new FitReasonerVariant(
                "drifted-model", reasonerAlwaysReturning("POOR_FIT", List.of("STEM alignment")), true);

        DriftExperimentReport report = new DriftExperimentRunner(euc, dataset).run(baseline, List.of(driftedVariant));

        assertEquals(1.0, report.driftDetectionRate(), "the one altering variant should have been flagged");
        assertTrue(Double.isNaN(report.falseFlagRate()), "no neutral variants were run");
        assertEquals(1, report.variantResults().size());
        assertTrue(report.variantResults().get(0).anyDriftFlagged());
    }

    @Test
    void behaviorNeutralVariantThatAgreesWithGroundTruthIsNotFlagged() {
        List<TestCase> dataset = List.of(eligibleGroundTruth("case-1", "STRONG_FIT", List.of("STEM")));

        FitReasonerVariant baseline = new FitReasonerVariant(
                "baseline", reasonerAlwaysReturning("STRONG_FIT", List.of("STEM alignment")), false);
        FitReasonerVariant neutralVariant = new FitReasonerVariant(
                "harmless-prompt-tweak", reasonerAlwaysReturning("STRONG_FIT", List.of("STEM alignment")), false);

        DriftExperimentReport report = new DriftExperimentRunner(euc, dataset).run(baseline, List.of(neutralVariant));

        assertEquals(0.0, report.falseFlagRate(), "the neutral variant should not have been flagged");
        assertTrue(Double.isNaN(report.driftDetectionRate()), "no altering variants were run");
        assertFalse(report.variantResults().get(0).anyDriftFlagged());
    }

    @Test
    void deterministicRuleStabilityHoldsWhenOnlyReasoningLayerChanges() {
        // eligibility/geography/requiredInfo never touch the FitReasoner, so
        // eligibilityCorrectness must stay identical across every variant
        // regardless of what the reasoner returns.
        List<TestCase> dataset = List.of(eligibleGroundTruth("case-1", "STRONG_FIT", List.of()));

        FitReasonerVariant baseline = new FitReasonerVariant(
                "baseline", reasonerAlwaysReturning("STRONG_FIT", List.of()), false);
        FitReasonerVariant variant = new FitReasonerVariant(
                "any-reasoning-variant", reasonerAlwaysReturning("POOR_FIT", List.of()), true);

        DriftExperimentReport report = new DriftExperimentRunner(euc, dataset).run(baseline, List.of(variant));

        assertEquals(1.0, report.deterministicRuleStabilityRate(),
                "eligibilityCorrectness must not move when only alignmentReasoning changes");
    }

    @Test
    void evidenceGroundingConsistencyReflectsFractionOfGroundedEligibleCases() {
        List<TestCase> dataset = List.of(
                eligibleGroundTruth("grounded", "STRONG_FIT", List.of("STEM")),
                eligibleGroundTruth("ungrounded", "STRONG_FIT", List.of("STEM"))
        );

        FitReasonerVariant baseline = new FitReasonerVariant(
                "baseline", reasonerAlwaysReturning("STRONG_FIT", List.of("STEM alignment")), false);

        // A variant that only ever cites unrelated evidence -> fails
        // evidenceGrounding on both cases -> consistency rate should be 0.0.
        FitReasonerVariant ungroundedVariant = new FitReasonerVariant(
                "ungrounded-variant", reasonerAlwaysReturning("STRONG_FIT", List.of("unrelated note")), true);

        DriftExperimentReport report = new DriftExperimentRunner(euc, dataset).run(baseline, List.of(ungroundedVariant));

        assertEquals(0.0, report.evidenceGroundingConsistencyRate());
    }
}
