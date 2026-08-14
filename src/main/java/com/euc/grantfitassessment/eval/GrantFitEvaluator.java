package com.euc.grantfitassessment.eval;

import com.euc.core.EucDefinition;
import com.euc.core.EvaluationFilter;
import com.euc.core.EvaluationFilterRegistry;
import com.euc.core.EvaluationPipelineBuilder;
import com.euc.core.PipelineContext;
import com.euc.grantfitassessment.AssessmentResult;
import com.euc.grantfitassessment.eval.pipeline.EligibilityCorrectnessFilter;
import com.euc.grantfitassessment.eval.pipeline.EvidenceGroundingFilter;
import com.euc.grantfitassessment.eval.pipeline.GrantFitEvalContextKeys;
import com.euc.grantfitassessment.eval.pipeline.ProgramAlignmentFilter;
import com.euc.grantfitassessment.pipeline.GrantFitContextKeys;

import java.util.List;
import java.util.Map;

/**
 * Scores an AssessmentResult against a TestCase's ground truth, using the
 * three criteria declared in the EUC's evaluationPipeline:
 * eligibilityCorrectness, programAlignment, evidenceGrounding.
 *
 * Scoring is driven by EvaluationPipelineBuilder from the EUC's
 * evaluationPipeline — each stage's `filter` key resolves through an
 * EvaluationFilterRegistry to a filter class in
 * com.euc.grantfitassessment.eval.pipeline — mirroring how GrantFitApplication
 * drives execution from executionPipeline (see docs/proposal.md Section 3:
 * the same schema-driven pattern covers both halves).
 */
public class GrantFitEvaluator {

    private final EvaluationPipelineBuilder pipelineBuilder;

    public GrantFitEvaluator(EucDefinition euc) {
        EvaluationFilterRegistry registry = new EvaluationFilterRegistry()
                .register("eligibilityCorrectness", new EligibilityCorrectnessFilter())
                .register("programAlignment", new ProgramAlignmentFilter())
                .register("evidenceGrounding", new EvidenceGroundingFilter());

        this.pipelineBuilder = new EvaluationPipelineBuilder(euc, registry);
    }

    @SuppressWarnings("unchecked")
    public EvaluationScore evaluate(TestCase testCase, AssessmentResult actual) {
        PipelineContext context = new PipelineContext();
        context.put(GrantFitContextKeys.ELIGIBLE, actual.eligible());
        context.put(GrantFitContextKeys.FIT_CLASSIFICATION, actual.fitClassification());
        context.put(GrantFitContextKeys.SUPPORTING_EVIDENCE, actual.supportingEvidence());
        context.put(GrantFitEvalContextKeys.EXPECTED_ELIGIBLE, testCase.expectedEligible());
        context.put(GrantFitEvalContextKeys.EXPECTED_FIT_CLASSIFICATION, testCase.expectedFitClassification());
        context.put(GrantFitEvalContextKeys.EXPECTED_EVIDENCE_KEYWORDS, testCase.expectedEvidenceKeywords());
        context.put(GrantFitEvalContextKeys.MISSING_EVIDENCE_KEYWORDS, List.of());

        Map<String, EvaluationFilter.Verdict> verdicts = pipelineBuilder.run(context);
        List<String> missingEvidence = context.get(GrantFitEvalContextKeys.MISSING_EVIDENCE_KEYWORDS, List.class);

        return new EvaluationScore(
                testCase.caseId(),
                verdicts.get("eligibilityCorrectness") == EvaluationFilter.Verdict.PASSED,
                verdicts.get("programAlignment") == EvaluationFilter.Verdict.PASSED,
                verdicts.get("evidenceGrounding") == EvaluationFilter.Verdict.PASSED,
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
