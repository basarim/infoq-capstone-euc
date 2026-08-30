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
 * evaluation criteria the EUC declares.
 *
 * Each evaluator is registered against a criterion id, and each criterion
 * declares through `tracesTo` which requirements, rules and policies it
 * validates — so a verdict here can be followed back to the business
 * requirement it speaks to, which is the property this project exists to
 * test.
 *
 * In the full design these evaluators are where an existing framework
 * (DeepEval, Ragas, an LLM-as-a-judge rubric) would plug in; the prototype
 * implements the assertions directly. See docs/proposal.md Section 5.
 */
public class GrantFitEvaluator {

    /** Criterion ids, as declared in grant-fit-assessment.json. */
    private static final String ELIGIBILITY = "EVAL-ELIGIBILITY";
    private static final String ALIGNMENT = "EVAL-ALIGNMENT";
    private static final String EVIDENCE = "EVAL-EVIDENCE";

    private final EvaluationPipelineBuilder pipelineBuilder;

    public GrantFitEvaluator(EucDefinition euc) {
        EvaluationFilterRegistry registry = new EvaluationFilterRegistry()
                .register(ELIGIBILITY, new EligibilityCorrectnessFilter())
                .register(ALIGNMENT, new ProgramAlignmentFilter())
                .register(EVIDENCE, new EvidenceGroundingFilter());

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
                verdicts.get(ELIGIBILITY) == EvaluationFilter.Verdict.PASSED,
                verdicts.get(ALIGNMENT) == EvaluationFilter.Verdict.PASSED,
                verdicts.get(EVIDENCE) == EvaluationFilter.Verdict.PASSED,
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
