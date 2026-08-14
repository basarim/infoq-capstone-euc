package com.euc.grantfitassessment.eval.pipeline;

/**
 * Ground-truth and output field names used alongside GrantFitContextKeys
 * when scoring an AssessmentResult against a TestCase. These are eval-only
 * fields — not part of the EUC's declared context — since ground truth is
 * established independently of the application (docs/proposal.md Section 6
 * step 2), not something an execution stage writes.
 */
public final class GrantFitEvalContextKeys {

    public static final String EXPECTED_ELIGIBLE = "expectedEligible";
    public static final String EXPECTED_FIT_CLASSIFICATION = "expectedFitClassification";
    public static final String EXPECTED_EVIDENCE_KEYWORDS = "expectedEvidenceKeywords";
    public static final String MISSING_EVIDENCE_KEYWORDS = "missingEvidenceKeywords";

    private GrantFitEvalContextKeys() {
    }
}
