from __future__ import annotations

from euc.core.loader import load_grant_fit_assessment
from euc.grantfitassessment.eval.dataset import TestCase
from euc.grantfitassessment.eval.evaluator import GrantFitEvaluator
from euc.grantfitassessment.models import AssessmentResult, GrantOpportunity, Organization

_evaluator = GrantFitEvaluator(load_grant_fit_assessment())


def _sample_test_case(expected_fit: str, expected_keywords: list[str]) -> TestCase:
    org = Organization(name="Org", mission_statement="Mission", programs=["Program"], operating_region="PNW", is_registered_nonprofit=True)
    grant = GrantOpportunity(
        funder_name="Funder", grant_name="Grant", funding_priorities=["Priority"],
        eligibility_requirements=[], allowed_regions=["PNW"], requires_registered_nonprofit=True,
    )
    return TestCase(
        case_id="case-1",
        organization=org,
        grant=grant,
        expected_eligible=True,
        expected_fit_classification=expected_fit,
        ground_truth_rationale="rationale",
        expected_evidence_keywords=expected_keywords,
    )


def test_matching_result_passes_all_criteria():
    tc = _sample_test_case("STRONG_FIT", ["STEM"])
    actual = AssessmentResult(
        eligible=True, failed_eligibility_rules=[], fit_classification="STRONG_FIT",
        explanation="explanation", supporting_evidence=["Strong STEM alignment"], identified_uncertainty=[],
    )

    score = _evaluator.evaluate(tc, actual)
    assert score.all_passed()


def test_mismatched_classification_fails_program_alignment():
    tc = _sample_test_case("STRONG_FIT", [])
    actual = AssessmentResult(
        eligible=True, failed_eligibility_rules=[], fit_classification="POOR_FIT",
        explanation="explanation", supporting_evidence=[], identified_uncertainty=[],
    )

    score = _evaluator.evaluate(tc, actual)
    assert not score.program_alignment
    assert not score.all_passed()


def test_missing_expected_evidence_fails_evidence_grounding():
    tc = _sample_test_case("STRONG_FIT", ["STEM", "underserved"])
    actual = AssessmentResult(
        eligible=True, failed_eligibility_rules=[], fit_classification="STRONG_FIT",
        explanation="explanation", supporting_evidence=["Some unrelated evidence"], identified_uncertainty=[],
    )

    score = _evaluator.evaluate(tc, actual)
    assert not score.evidence_grounding
    assert set(["STEM", "underserved"]).issubset(set(score.missing_evidence_keywords))


def test_eligibility_mismatch_fails_eligibility_correctness():
    tc = _sample_test_case("STRONG_FIT", [])
    actual = AssessmentResult(
        eligible=False, failed_eligibility_rules=["ELIGIBILITY-001"], fit_classification="POOR_FIT",
        explanation="not eligible", supporting_evidence=[], identified_uncertainty=[],
    )

    score = _evaluator.evaluate(tc, actual)
    assert not score.eligibility_correctness
