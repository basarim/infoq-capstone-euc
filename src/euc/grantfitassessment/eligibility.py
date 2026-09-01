"""A standalone deterministic eligibility rule engine, tested independently of
the pipeline. Unlike the pipeline filters (which each halt the run on their
own failure), this accumulates every failure so all of them can be reported
at once — useful outside the halt-on-first-failure pipeline contract."""

from __future__ import annotations

from euc.grantfitassessment.models import GrantOpportunity, Organization


def _java_list_str(items: list[str]) -> str:
    return "[" + ", ".join(items) + "]"


def check_eligibility(org: Organization, grant: GrantOpportunity) -> list[str]:
    failures: list[str] = []

    if grant.requires_registered_nonprofit and not org.is_registered_nonprofit:
        failures.append("ELIGIBILITY-001: organization is not a registered nonprofit")

    if grant.allowed_regions and org.operating_region not in grant.allowed_regions:
        failures.append(
            f"GEOGRAPHY-001: operating region '{org.operating_region}' is outside the "
            f"grant's allowed regions {_java_list_str(grant.allowed_regions)}"
        )

    if not org.mission_statement or not org.mission_statement.strip():
        failures.append("INFO-001: organization mission statement is missing")
    if not org.programs:
        failures.append("INFO-001: organization programs are missing")

    return failures
