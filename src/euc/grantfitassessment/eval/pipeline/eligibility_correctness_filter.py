"""EVAL-ELIGIBILITY — pure equality against ground truth, no leniency."""

from __future__ import annotations

from euc.core.context import PipelineContext
from euc.core.filters import Verdict
from euc.core.models import EvaluationCriterion
from euc.grantfitassessment import context_keys as prod_keys
from euc.grantfitassessment.eval import context_keys as eval_keys


def eligibility_correctness_filter(context: PipelineContext, criterion: EvaluationCriterion) -> Verdict:
    actual: bool = context.get(prod_keys.ELIGIBLE)
    expected: bool = context.get(eval_keys.EXPECTED_ELIGIBLE)
    return Verdict.PASSED if actual == expected else Verdict.FAILED
