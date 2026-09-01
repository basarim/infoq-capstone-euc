from __future__ import annotations

from dataclasses import dataclass, field

from euc.core.context import PipelineContext
from euc.core.filters import EvaluationFilterRegistry, Verdict
from euc.core.models import EucDefinition
from euc.core.pipeline import EvaluationPipelineBuilder
from euc.grantfitassessment import context_keys as prod_keys
from euc.grantfitassessment.eval import context_keys as eval_keys
from euc.grantfitassessment.eval.dataset import TestCase
from euc.grantfitassessment.eval.pipeline.eligibility_correctness_filter import (
    eligibility_correctness_filter,
)
from euc.grantfitassessment.eval.pipeline.evidence_grounding_filter import evidence_grounding_filter
from euc.grantfitassessment.eval.pipeline.program_alignment_filter import program_alignment_filter
from euc.grantfitassessment.models import AssessmentResult

ELIGIBILITY = "EVAL-ELIGIBILITY"
ALIGNMENT = "EVAL-ALIGNMENT"
EVIDENCE = "EVAL-EVIDENCE"


@dataclass(frozen=True)
class EvaluationScore:
    case_id: str
    eligibility_correctness: bool
    program_alignment: bool
    evidence_grounding: bool
    missing_evidence_keywords: list[str] = field(default_factory=list)

    def all_passed(self) -> bool:
        return self.eligibility_correctness and self.program_alignment and self.evidence_grounding


class GrantFitEvaluator:
    def __init__(self, euc: EucDefinition) -> None:
        registry = EvaluationFilterRegistry()
        registry.register(ELIGIBILITY, eligibility_correctness_filter)
        registry.register(ALIGNMENT, program_alignment_filter)
        registry.register(EVIDENCE, evidence_grounding_filter)
        self._pipeline_builder = EvaluationPipelineBuilder(euc, registry)

    def evaluate(self, test_case: TestCase, actual: AssessmentResult) -> EvaluationScore:
        context = PipelineContext()
        context.put(prod_keys.ELIGIBLE, actual.eligible)
        context.put(prod_keys.FIT_CLASSIFICATION, actual.fit_classification)
        context.put(prod_keys.SUPPORTING_EVIDENCE, actual.supporting_evidence)
        context.put(eval_keys.EXPECTED_ELIGIBLE, test_case.expected_eligible)
        context.put(eval_keys.EXPECTED_FIT_CLASSIFICATION, test_case.expected_fit_classification)
        context.put(eval_keys.EXPECTED_EVIDENCE_KEYWORDS, test_case.expected_evidence_keywords)
        context.put(eval_keys.MISSING_EVIDENCE_KEYWORDS, [])

        verdicts = self._pipeline_builder.run(context)
        missing_evidence = context.get(eval_keys.MISSING_EVIDENCE_KEYWORDS)

        return EvaluationScore(
            case_id=test_case.case_id,
            eligibility_correctness=verdicts[ELIGIBILITY] == Verdict.PASSED,
            program_alignment=verdicts[ALIGNMENT] == Verdict.PASSED,
            evidence_grounding=verdicts[EVIDENCE] == Verdict.PASSED,
            missing_evidence_keywords=missing_evidence,
        )
