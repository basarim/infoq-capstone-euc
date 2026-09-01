"""Tests for the deterministic layer only — no LLM call involved, so these
should stay green regardless of any prompt or model change (that stability
is the point per docs/proposal.md Section 4's secondary claim)."""

from __future__ import annotations

from euc.grantfitassessment.eligibility import check_eligibility
from euc.grantfitassessment.models import GrantOpportunity, Organization


def _standard_grant() -> GrantOpportunity:
    return GrantOpportunity(
        funder_name="Evergreen Community Foundation",
        grant_name="Youth Education Grant",
        funding_priorities=["STEM education", "Underserved communities"],
        eligibility_requirements=["Registered 501(c)(3)", "Operating in Pacific Northwest"],
        allowed_regions=["Pacific Northwest"],
        requires_registered_nonprofit=True,
    )


def test_eligible_organization_passes_all_rules():
    org = Organization(
        name="Riverside Youth Coalition",
        mission_statement="Providing after-school STEM programs to underserved youth.",
        programs=["STEM tutoring"],
        operating_region="Pacific Northwest",
        is_registered_nonprofit=True,
    )

    failures = check_eligibility(org, _standard_grant())
    assert failures == [], f"expected no eligibility failures, got: {failures}"


def test_non_registered_nonprofit_fails_eligibility_001():
    org = Organization(
        name="NextGen Learning Co.",
        mission_statement="For-profit tutoring services.",
        programs=["Paid STEM tutoring"],
        operating_region="Pacific Northwest",
        is_registered_nonprofit=False,
    )

    failures = check_eligibility(org, _standard_grant())
    assert any(f.startswith("ELIGIBILITY-001") for f in failures)


def test_out_of_region_organization_fails_geography_001():
    org = Organization(
        name="Coastal STEM Academy Trust",
        mission_statement="Delivering STEM education to underserved youth.",
        programs=["STEM tutoring"],
        operating_region="Northeast",
        is_registered_nonprofit=True,
    )

    failures = check_eligibility(org, _standard_grant())
    assert any(f.startswith("GEOGRAPHY-001") for f in failures)


def test_missing_mission_statement_fails_info_001():
    org = Organization(
        name="Unnamed Org",
        mission_statement="",
        programs=["Some program"],
        operating_region="Pacific Northwest",
        is_registered_nonprofit=True,
    )

    failures = check_eligibility(org, _standard_grant())
    assert any(f.startswith("INFO-001") for f in failures)


def test_missing_programs_fails_info_001():
    org = Organization(
        name="Unnamed Org",
        mission_statement="A mission statement.",
        programs=[],
        operating_region="Pacific Northwest",
        is_registered_nonprofit=True,
    )

    failures = check_eligibility(org, _standard_grant())
    assert any(f.startswith("INFO-001") for f in failures)


def test_multiple_failures_are_all_reported():
    org = Organization(
        name="Unnamed Org",
        mission_statement="",
        programs=[],
        operating_region="Northeast",
        is_registered_nonprofit=False,
    )

    failures = check_eligibility(org, _standard_grant())
    assert len(failures) == 4, f"expected all four rule failures, got: {failures}"
