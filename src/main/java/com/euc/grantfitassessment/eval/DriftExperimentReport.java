package com.euc.grantfitassessment.eval;

import java.util.List;
import java.util.function.Predicate;

/**
 * Aggregate results of a DriftExperimentRunner run: the four metrics from
 * docs/proposal.md Section 7, computed across every candidate variant
 * against the baseline.
 *
 * | Metric | Definition here |
 * |---|---|
 * | driftDetectionRate | % of expectedToAlterBehavior=true variants where evaluation flagged at least one case |
 * | falseFlagRate | % of expectedToAlterBehavior=false variants where evaluation flagged at least one case anyway |
 * | deterministicRuleStabilityRate | % of all variants whose eligibilityCorrectness verdicts never moved from baseline |
 * | evidenceGroundingConsistencyRate | mean, across variants, of the fraction of eligible cases whose evidenceGrounding passed |
 *
 * Pass/fail criteria (Section 7): drift-detection-rate high, false-flag-rate
 * low, deterministic-rule-stability at 1.0. A NaN rate means no variants of
 * that category were run — add candidates rather than read it as 0%.
 */
public record DriftExperimentReport(
        FitReasonerVariant baseline,
        List<DriftExperimentRunner.VariantResult> variantResults,
        double driftDetectionRate,
        double falseFlagRate,
        double deterministicRuleStabilityRate,
        double evidenceGroundingConsistencyRate
) {

    public static DriftExperimentReport from(FitReasonerVariant baseline,
                                              List<DriftExperimentRunner.VariantResult> variantResults) {
        List<DriftExperimentRunner.VariantResult> alteringVariants = variantResults.stream()
                .filter(r -> r.variant().expectedToAlterBehavior())
                .toList();
        List<DriftExperimentRunner.VariantResult> neutralVariants = variantResults.stream()
                .filter(r -> !r.variant().expectedToAlterBehavior())
                .toList();

        return new DriftExperimentReport(
                baseline,
                variantResults,
                rateOf(alteringVariants, DriftExperimentRunner.VariantResult::anyDriftFlagged),
                rateOf(neutralVariants, DriftExperimentRunner.VariantResult::anyDriftFlagged),
                rateOf(variantResults, DriftExperimentRunner.VariantResult::deterministicRuleStable),
                averageOf(variantResults, DriftExperimentRunner.VariantResult::evidenceGroundingRate)
        );
    }

    private static double rateOf(List<DriftExperimentRunner.VariantResult> results,
                                  Predicate<DriftExperimentRunner.VariantResult> predicate) {
        if (results.isEmpty()) {
            return Double.NaN;
        }
        long matching = results.stream().filter(predicate).count();
        return (double) matching / results.size();
    }

    private static double averageOf(List<DriftExperimentRunner.VariantResult> results,
                                     java.util.function.ToDoubleFunction<DriftExperimentRunner.VariantResult> fn) {
        double[] values = results.stream()
                .mapToDouble(fn)
                .filter(v -> !Double.isNaN(v))
                .toArray();
        if (values.length == 0) {
            return Double.NaN;
        }
        return java.util.Arrays.stream(values).average().orElse(Double.NaN);
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Drift experiment — baseline: ").append(baseline.label()).append("\n");
        sb.append(String.format("  drift-detection-rate:              %s%n", pct(driftDetectionRate)));
        sb.append(String.format("  false-flag-rate:                   %s%n", pct(falseFlagRate)));
        sb.append(String.format("  deterministic-rule-stability-rate: %s%n", pct(deterministicRuleStabilityRate)));
        sb.append(String.format("  evidence-grounding-consistency:    %s%n", pct(evidenceGroundingConsistencyRate)));
        sb.append("\nPer-variant:\n");
        for (DriftExperimentRunner.VariantResult r : variantResults) {
            sb.append(String.format("  %-40s expectedToAlterBehavior=%-5s flagged=%s stable=%s groundingRate=%s%n",
                    r.variant().label(), r.variant().expectedToAlterBehavior(),
                    r.flaggedCaseIds(), r.deterministicRuleStable(), pct(r.evidenceGroundingRate())));
        }
        return sb.toString();
    }

    private static String pct(double rate) {
        return Double.isNaN(rate) ? "n/a (no variants in this category)" : String.format("%.1f%%", rate * 100);
    }
}
