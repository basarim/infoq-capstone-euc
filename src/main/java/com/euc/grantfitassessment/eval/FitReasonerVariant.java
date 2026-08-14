package com.euc.grantfitassessment.eval;

import com.euc.grantfitassessment.FitReasoner;

/**
 * One configuration under test in the drift experiment (docs/proposal.md
 * Section 7): a FitReasoner implementation (a specific model + prompt)
 * plus whether this change is expected — independently of the experiment's
 * outcome — to alter reasoning behavior. That expectation is the "known to
 * alter behavior" comparison point Section 7 step 5 measures detection
 * against, so it must be set deliberately by whoever designs the variant
 * (e.g. a genuinely different model = true, a harmless refactor of prompt
 * wording that shouldn't change meaning = false), not inferred from
 * DriftExperimentRunner's results.
 */
public record FitReasonerVariant(String label, FitReasoner reasoner, boolean expectedToAlterBehavior) {
}
