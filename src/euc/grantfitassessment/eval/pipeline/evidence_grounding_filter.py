"""EVAL-EVIDENCE — every expected keyword must appear as a case-insensitive
substring of at least one supporting-evidence entry. Same vacuous-pass-on-
ineligible rule as EVAL-ALIGNMENT."""

from __future__ import annotations

from euc.core.context import PipelineContext
from euc.core.filters import Verdict
from euc.core.models import EvaluationCriterion
from euc.grantfitassessment import context_keys as prod_keys
from euc.grantfitassessment.eval import context_keys as eval_keys


def evidence_grounding_filter(context: PipelineContext, criterion: EvaluationCriterion) -> Verdict:
    actual_eligible: bool = context.get(prod_keys.ELIGIBLE)
    if not actual_eligible:
        context.put(eval_keys.MISSING_EVIDENCE_KEYWORDS, [])
        return Verdict.PASSED

    evidence: list[str] = context.get(prod_keys.SUPPORTING_EVIDENCE)
    expected_keywords: list[str] = context.get(eval_keys.EXPECTED_EVIDENCE_KEYWORDS)

    missing = [
        keyword
        for keyword in expected_keywords
        if not any(keyword.lower() in e.lower() for e in evidence)
    ]

    context.put(eval_keys.MISSING_EVIDENCE_KEYWORDS, missing)
    return Verdict.PASSED if not missing else Verdict.FAILED
