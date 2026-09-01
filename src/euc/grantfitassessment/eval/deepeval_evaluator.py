"""A drop-in alternative to GrantFitEvaluator (euc.grantfitassessment.eval.evaluator),
backed by DeepEval instead of the bespoke EvaluationPipelineBuilder. Same public
shape — evaluate(test_case, actual) -> EvaluationScore — proving the EUC's
evaluation criteria stay fixed while the framework underneath changes.

Doesn't reuse EvaluationPipelineBuilder/EvaluationFilterRegistry: those are typed
around the bespoke Verdict enum and PipelineContext, which don't map cleanly onto
DeepEval's own LLMTestCase/BaseMetric objects. Building one LLMTestCase per
evaluation and calling the three metrics directly is simpler and avoids forcing
DeepEval's types through an adapter layer that would add complexity without
adding fidelity.
"""

from __future__ import annotations

from deepeval.test_case import LLMTestCase

from euc.core.models import EucDefinition
from euc.grantfitassessment.eval.dataset import TestCase
from euc.grantfitassessment.eval.deepeval.eligibility_correctness_metric import (
    EligibilityCorrectnessMetric,
)
from euc.grantfitassessment.eval.deepeval.evidence_grounding_metric import (
    build_evidence_grounding_metric,
    evidence_grounding_test_case,
)
from euc.grantfitassessment.eval.deepeval.program_alignment_metric import ProgramAlignmentMetric
from euc.grantfitassessment.eval.evaluator import EvaluationScore
from euc.grantfitassessment.models import AssessmentResult


class DeepEvalGrantFitEvaluator:
    def __init__(self, euc: EucDefinition, model_name: str) -> None:
        self._euc = euc
        self._eligibility_metric = EligibilityCorrectnessMetric()
        self._alignment_metric = ProgramAlignmentMetric()
        self._evidence_metric = build_evidence_grounding_metric(model_name)

    def evaluate(self, test_case: TestCase, actual: AssessmentResult) -> EvaluationScore:
        deterministic_case = LLMTestCase(
            input=self._describe(test_case),
            actual_output=actual.fit_classification,
            expected_output=test_case.expected_fit_classification,
            metadata={
                "eligible": actual.eligible,
                "expected_eligible": test_case.expected_eligible,
                "fit_classification": actual.fit_classification,
            },
        )

        self._eligibility_metric.measure(deterministic_case)
        self._alignment_metric.measure(deterministic_case)

        if actual.eligible:
            evidence_case = evidence_grounding_test_case(
                self._describe(test_case), actual.explanation, actual.supporting_evidence
            )
            self._evidence_metric.measure(evidence_case)
            evidence_grounding = bool(self._evidence_metric.is_successful())
        else:
            # Ineligible — fit reasoning never ran, so there is no explanation to
            # judge. Vacuous pass, same rule as the bespoke evaluator, and it
            # skips a live model call that would have nothing to grade.
            evidence_grounding = True

        return EvaluationScore(
            case_id=test_case.case_id,
            eligibility_correctness=bool(self._eligibility_metric.is_successful()),
            program_alignment=bool(self._alignment_metric.is_successful()),
            evidence_grounding=evidence_grounding,
            # DeepEval's GEval judgment is holistic, not a keyword diff, so
            # there is nothing meaningful to report here — unlike the bespoke
            # evaluator's substring check.
            missing_evidence_keywords=[],
        )

    @staticmethod
    def _describe(test_case: TestCase) -> str:
        return (
            f"Assess whether {test_case.organization.name} is eligible for and a good "
            f"fit for {test_case.grant.grant_name}, funded by {test_case.grant.funder_name}."
        )
