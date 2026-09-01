"""End-to-end test of the EUC-driven run: EucLoader -> GrantFitApplication ->
PipelineBuilder -> ExecutionFilterRegistry -> implementations, using a fake
FitReasoner so it runs offline without ANTHROPIC_API_KEY.

This is the proof that execution is assembled from the EUC's declared
execution requirements rather than hand-wired, and that the halt contract is
honoured because the EUC says so. A live model call still needs a real key,
but the mechanics exercised here are exactly what a live call runs through.
"""

from __future__ import annotations

from euc.core.loader import load_grant_fit_assessment
from euc.grantfitassessment.app import GrantFitApplication
from euc.grantfitassessment.models import GrantOpportunity, Organization
from tests.grantfitassessment.fakes import FakeFitReasoner

_euc = load_grant_fit_assessment()


def _strong_fit_org() -> Organization:
    return Organization(
        name="Riverside Youth Coalition",
        mission_statement="Providing after-school STEM programs to underserved youth.",
        programs=["STEM tutoring", "College readiness workshops"],
        operating_region="Pacific Northwest",
        is_registered_nonprofit=True,
    )


def _standard_grant() -> GrantOpportunity:
    return GrantOpportunity(
        funder_name="Evergreen Community Foundation",
        grant_name="Youth Education Grant",
        funding_priorities=["STEM education", "Underserved communities"],
        eligibility_requirements=["Registered 501(c)(3)", "Operating in Pacific Northwest"],
        allowed_regions=["Pacific Northwest"],
        requires_registered_nonprofit=True,
    )


def test_happy_path_eligible_org_reaches_reasoner_and_returns_its_classification():
    reasoner = FakeFitReasoner.returning(
        "STRONG_FIT",
        "Mission and programs directly match funder priorities.",
        ["Strong STEM alignment", "Serves underserved youth"],
        [],
    )
    app = GrantFitApplication(_euc, reasoner)

    result = app.assess(_strong_fit_org(), _standard_grant())

    assert result.eligible
    assert result.failed_eligibility_rules == []
    assert result.fit_classification == "STRONG_FIT"
    assert result.supporting_evidence == ["Strong STEM alignment", "Serves underserved youth"]
    assert reasoner.was_invoked(), "expected ALIGNMENT-001 to run once eligibility passed"


def test_ineligible_org_pipeline_halts_before_reasoning_runs():
    reasoner = FakeFitReasoner.that_must_not_be_invoked()
    app = GrantFitApplication(_euc, reasoner)

    not_nonprofit = Organization(
        name="NextGen Learning Co.",
        mission_statement="For-profit tutoring services for STEM subjects.",
        programs=["Paid STEM tutoring"],
        operating_region="Pacific Northwest",
        is_registered_nonprofit=False,
    )

    result = app.assess(not_nonprofit, _standard_grant())

    assert not result.eligible
    assert result.fit_classification == "POOR_FIT"
    assert any(f.startswith("ELIGIBILITY-001") for f in result.failed_eligibility_rules)
    assert not reasoner.was_invoked(), (
        "onFailure: halt on ELIGIBILITY-001 should stop the pipeline before ALIGNMENT-001"
    )


def test_out_of_region_org_halts_at_geography_stage():
    reasoner = FakeFitReasoner.that_must_not_be_invoked()
    app = GrantFitApplication(_euc, reasoner)

    out_of_region = Organization(
        name="Coastal STEM Academy Trust",
        mission_statement="Delivering STEM education to underserved youth on the East Coast.",
        programs=["STEM tutoring", "Robotics club"],
        operating_region="Northeast",
        is_registered_nonprofit=True,
    )

    result = app.assess(out_of_region, _standard_grant())

    assert not result.eligible
    assert any(f.startswith("GEOGRAPHY-001") for f in result.failed_eligibility_rules)
    assert not reasoner.was_invoked()
