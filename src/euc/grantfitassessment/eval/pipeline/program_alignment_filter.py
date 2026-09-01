"""EVAL-ALIGNMENT — checked against the assessment's own (actual) eligibility,
not the ground-truth eligibility: an ineligible verdict vacuously passes,
since fit reasoning never ran to produce one worth checking."""

from __future__ import annotations

from euc.core.context import PipelineContext
from euc.core.filters import Verdict
from euc.core.models import EvaluationCriterion
from euc.grantfitassessment import context_keys as prod_keys
from euc.grantfitassessment.eval import context_keys as eval_keys


def program_alignment_filter(context: PipelineContext, criterion: EvaluationCriterion) -> Verdict:
    actual_eligible: bool = context.get(prod_keys.ELIGIBLE)
    if not actual_eligible:
        return Verdict.PASSED

    actual_fit: str = context.get(prod_keys.FIT_CLASSIFICATION)
    expected_fit: str = context.get(eval_keys.EXPECTED_FIT_CLASSIFICATION)
    return Verdict.PASSED if expected_fit == actual_fit else Verdict.FAILED
