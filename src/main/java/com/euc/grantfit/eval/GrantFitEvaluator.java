package com.euc.grantfit.eval;

import com.euc.grantfit.AssessmentResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores an AssessmentResult against a TestCase's ground truth, using the
 * three criteria declared in the EUC's "evaluation" field:
 * eligibilityCorrectness, programAlignment, evidenceGrounding.
 *
 * This evaluator is bound to the same EUC the application reads (see
 * docs/proposal.md Section 3) — it is not a separately authored test
 * suite that happens to cover similar ground.
 */
public class GrantFitEvaluator {

    public EvaluationScore evaluate(TestCase testCase, AssessmentResult actual) {
        boolean eligibilityCorrect = actual.eligible() == testCase.expectedEligible();

        boolean programAlignmentCorrect = !actual.eligible()
                ? true // eligibility failure short-circuits fit; nothing to score here
                : testCase.expectedFitClassification().equals(actual.fitClassification());

        List<String> missingEvidence = new ArrayList<>();
        if (actual.eligible()) {
            for (String keyword : testCase.expectedEvidenceKeywords()) {
                boolean found = actual.supportingEvidence().stream()
                        .anyMatch(e -> e.toLowerCase().contains(keyword.toLowerCase()));
                if (!found) {
                    missingEvidence.add(keyword);
                }
            }
        }
        boolean evidenceGrounded = missingEvidence.isEmpty();

        return new EvaluationScore(
                testCase.caseId(),
                eligibilityCorrect,
                programAlignmentCorrect,
                evidenceGrounded,
                missingEvidence
        );
    }

    public record EvaluationScore(
            String caseId,
            boolean eligibilityCorrectness,
            boolean programAlignment,
            boolean evidenceGrounding,
            List<String> missingEvidenceKeywords
    ) {
        public boolean allPassed() {
            return eligibilityCorrectness && programAlignment && evidenceGrounding;
        }
    }
}
