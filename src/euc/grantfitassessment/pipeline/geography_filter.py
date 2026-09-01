"""GEOGRAPHY-001 — the organization must operate inside the grant's allowed regions."""

from __future__ import annotations

from euc.core.context import PipelineContext
from euc.core.filters import Outcome
from euc.core.models import ExecutionRequirement
from euc.grantfitassessment import context_keys as keys
from euc.grantfitassessment.models import GrantOpportunity, Organization


def _java_list_str(items: list[str]) -> str:
    return "[" + ", ".join(items) + "]"


def geography_rule_filter(context: PipelineContext, stage: ExecutionRequirement) -> Outcome:
    org: Organization = context.get(keys.ORGANIZATION)
    grant: GrantOpportunity = context.get(keys.GRANT)

    if grant.allowed_regions and org.operating_region not in grant.allowed_regions:
        context.put(keys.ELIGIBLE, False)
        context.put(
            keys.FAILED_ELIGIBILITY_RULES,
            [
                f"{stage.id}: operating region '{org.operating_region}' is outside the "
                f"grant's allowed regions {_java_list_str(grant.allowed_regions)}"
            ],
        )
        return Outcome.FAILED

    context.put(keys.ELIGIBLE, True)
    return Outcome.PASSED
