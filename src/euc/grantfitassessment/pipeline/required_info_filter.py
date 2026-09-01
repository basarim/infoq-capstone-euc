"""INFO-001 — the organization's mission and programs must be present before
alignment reasoning can run against them."""

from __future__ import annotations

from euc.core.context import PipelineContext
from euc.core.filters import Outcome
from euc.core.models import ExecutionRequirement
from euc.grantfitassessment import context_keys as keys
from euc.grantfitassessment.models import Organization


def required_info_rule_filter(context: PipelineContext, stage: ExecutionRequirement) -> Outcome:
    org: Organization = context.get(keys.ORGANIZATION)

    failures: list[str] = []
    if not org.mission_statement or not org.mission_statement.strip():
        failures.append(f"{stage.id}: organization mission statement is missing")
    if not org.programs:
        failures.append(f"{stage.id}: organization programs are missing")

    if failures:
        context.put(keys.ELIGIBLE, False)
        context.put(keys.FAILED_ELIGIBILITY_RULES, failures)
        return Outcome.FAILED

    context.put(keys.ELIGIBLE, True)
    return Outcome.PASSED
