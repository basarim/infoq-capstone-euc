"""ALIGNMENT-001 — the reasoned step, reached only once every deterministic
gate above has passed."""

from __future__ import annotations

from euc.core.context import PipelineContext
from euc.core.filters import Outcome
from euc.core.models import ExecutionRequirement
from euc.core.models import EucDefinition
from euc.grantfitassessment import context_keys as keys
from euc.grantfitassessment.models import GrantOpportunity, Organization
from euc.grantfitassessment.reasoner import FitReasoner


class AlignmentReasoningFilter:
    def __init__(self, reasoner: FitReasoner, euc: EucDefinition) -> None:
        self._reasoner = reasoner
        self._euc = euc

    def __call__(self, context: PipelineContext, stage: ExecutionRequirement) -> Outcome:
        org: Organization = context.get(keys.ORGANIZATION)
        grant: GrantOpportunity = context.get(keys.GRANT)

        reasoning = self._reasoner.assess_fit(org, grant, self._euc)

        context.put(keys.FIT_CLASSIFICATION, reasoning.fit_classification)
        context.put(keys.EXPLANATION, reasoning.explanation)
        context.put(keys.SUPPORTING_EVIDENCE, reasoning.supporting_evidence)
        context.put(keys.IDENTIFIED_UNCERTAINTY, reasoning.identified_uncertainty)

        return Outcome.PASSED
