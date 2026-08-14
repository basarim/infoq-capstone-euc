package com.euc.grantfitassessment.eval;

import com.euc.core.EucDefinition;
import com.euc.grantfitassessment.AssessmentResult;
import com.euc.grantfitassessment.GrantFitApplication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the drift-detection experiment described in docs/proposal.md
 * Section 7: hold the EUC fixed, run the eval dataset against a baseline
 * FitReasoner and one or more candidate variants (model swaps, prompt
 * edits), and measure whether evaluation against the unchanged EUC still
 * tracks the resulting behavior.
 *
 * How "flagged" is operationalized here (see compare()) is a specific
 * design choice, not something the EUC schema dictates: a case counts as
 * flagged when the evaluator agreed with ground truth under the baseline
 * but stopped agreeing under the variant — i.e. evaluation caught a
 * behavior change on that datapoint. A study design that wants a stricter
 * or looser definition should adjust compare() rather than treat this as
 * fixed by the schema.
 */
public class DriftExperimentRunner {

    private final EucDefinition euc;
    private final List<TestCase> dataset;

    public DriftExperimentRunner(EucDefinition euc, List<TestCase> dataset) {
        this.euc = euc;
        this.dataset = dataset;
    }

    public DriftExperimentReport run(FitReasonerVariant baseline, List<FitReasonerVariant> candidates) {
        Map<String, CaseOutcome> baselineOutcomes = scoreVariant(baseline);

        List<VariantResult> variantResults = new ArrayList<>();
        for (FitReasonerVariant candidate : candidates) {
            Map<String, CaseOutcome> candidateOutcomes = scoreVariant(candidate);
            variantResults.add(compare(candidate, baselineOutcomes, candidateOutcomes));
        }

        return DriftExperimentReport.from(baseline, variantResults);
    }

    private Map<String, CaseOutcome> scoreVariant(FitReasonerVariant variant) {
        GrantFitApplication app = new GrantFitApplication(euc, variant.reasoner());
        GrantFitEvaluator evaluator = new GrantFitEvaluator(euc);

        Map<String, CaseOutcome> outcomes = new LinkedHashMap<>();
        for (TestCase tc : dataset) {
            AssessmentResult actual = app.assess(tc.organization(), tc.grant());
            GrantFitEvaluator.EvaluationScore score = evaluator.evaluate(tc, actual);
            outcomes.put(tc.caseId(), new CaseOutcome(tc.caseId(), actual, score));
        }
        return outcomes;
    }

    private VariantResult compare(FitReasonerVariant candidate,
                                   Map<String, CaseOutcome> baselineOutcomes,
                                   Map<String, CaseOutcome> candidateOutcomes) {
        List<String> flaggedCases = new ArrayList<>();
        List<String> eligibilityCorrectnessDriftCases = new ArrayList<>();
        int eligibleCases = 0;
        int groundedEligibleCases = 0;

        for (String caseId : baselineOutcomes.keySet()) {
            CaseOutcome baselineOutcome = baselineOutcomes.get(caseId);
            CaseOutcome candidateOutcome = candidateOutcomes.get(caseId);

            if (baselineOutcome.score().allPassed() && !candidateOutcome.score().allPassed()) {
                flaggedCases.add(caseId);
            }

            // deterministic-rule-stability: eligibilityCorrectness must not move
            // when only the reasoning layer (alignmentReasoning) changes, since
            // the deterministic filters (eligibility/geography/requiredInfo) are
            // untouched by any FitReasoner variant.
            if (baselineOutcome.score().eligibilityCorrectness() != candidateOutcome.score().eligibilityCorrectness()) {
                eligibilityCorrectnessDriftCases.add(caseId);
            }

            if (candidateOutcome.actual().eligible()) {
                eligibleCases++;
                if (candidateOutcome.score().evidenceGrounding()) {
                    groundedEligibleCases++;
                }
            }
        }

        double evidenceGroundingRate = eligibleCases == 0 ? Double.NaN : (double) groundedEligibleCases / eligibleCases;

        return new VariantResult(candidate, flaggedCases, eligibilityCorrectnessDriftCases,
                evidenceGroundingRate, candidateOutcomes);
    }

    /** One test case's outcome for a given variant: the raw result plus its score against ground truth. */
    public record CaseOutcome(String caseId, AssessmentResult actual, GrantFitEvaluator.EvaluationScore score) {
    }

    /** One candidate variant's comparison against the baseline, across the full dataset. */
    public record VariantResult(
            FitReasonerVariant variant,
            List<String> flaggedCaseIds,
            List<String> eligibilityCorrectnessDriftCaseIds,
            double evidenceGroundingRate,
            Map<String, CaseOutcome> outcomes
    ) {
        public boolean anyDriftFlagged() {
            return !flaggedCaseIds.isEmpty();
        }

        public boolean deterministicRuleStable() {
            return eligibilityCorrectnessDriftCaseIds.isEmpty();
        }
    }
}
