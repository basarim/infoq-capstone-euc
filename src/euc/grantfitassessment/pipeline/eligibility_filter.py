"""ELIGIBILITY-001 — mandatory registered-nonprofit status."""

from __future__ import annotations

from euc.core.context import PipelineContext
from euc.core.filters import Outcome
from euc.core.models import ExecutionRequirement
from euc.grantfitassessment import context_keys as keys
from euc.grantfitassessment.models import GrantOpportunity, Organization


def eligibility_rule_filter(context: PipelineContext, stage: ExecutionRequirement) -> Outcome:
    org: Organization = context.get(keys.ORGANIZATION)
    grant: GrantOpportunity = context.get(keys.GRANT)

    if grant.requires_registered_nonprofit and not org.is_registered_nonprofit:
        context.put(keys.ELIGIBLE, False)
        context.put(keys.FAILED_ELIGIBILITY_RULES, [f"{stage.id}: organization is not a registered nonprofit"])
        return Outcome.FAILED

    context.put(keys.ELIGIBLE, True)
    return Outcome.PASSED
