package com.euc.grantfitassessment.pipeline;

/**
 * Field names used in the PipelineContext for a Grant Fit Assessment run.
 * These must match the EUC's declared `seedFields` and each stage's
 * `reads`/`writes` exactly (src/main/resources/euc/grant-fit-assessment.json)
 * — the filters below are the code-side half of that contract.
 */
public final class GrantFitContextKeys {

    public static final String ORGANIZATION = "organization";
    public static final String GRANT = "grant";
    public static final String ELIGIBLE = "eligible";
    public static final String FAILED_ELIGIBILITY_RULES = "failedEligibilityRules";
    public static final String FIT_CLASSIFICATION = "fitClassification";
    public static final String EXPLANATION = "explanation";
    public static final String SUPPORTING_EVIDENCE = "supportingEvidence";
    public static final String IDENTIFIED_UNCERTAINTY = "identifiedUncertainty";

    private GrantFitContextKeys() {
    }
}
