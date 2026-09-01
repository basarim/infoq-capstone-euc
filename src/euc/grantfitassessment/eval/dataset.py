"""Loads the golden set: hand-authored ground truth, established
independently of the application by reading each organization/grant pair
against the EUC's rules and policies."""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path

from euc.grantfitassessment.models import GrantOpportunity, Organization

DATASET_PATH = "eval/grant-fit-assessment/dataset/test-cases.json"


@dataclass(frozen=True)
class TestCase:
    __test__ = False  # not a pytest test class, despite the name

    case_id: str
    organization: Organization
    grant: GrantOpportunity
    expected_eligible: bool
    expected_fit_classification: str
    ground_truth_rationale: str
    expected_evidence_keywords: list[str] = field(default_factory=list)


def _parse_test_case(data: dict) -> TestCase:
    return TestCase(
        case_id=data.get("caseId", ""),
        organization=Organization.from_dict(data.get("organization", {}) or {}),
        grant=GrantOpportunity.from_dict(data.get("grant", {}) or {}),
        expected_eligible=bool(data.get("expectedEligible", False)),
        expected_fit_classification=data.get("expectedFitClassification", ""),
        ground_truth_rationale=data.get("groundTruthRationale", ""),
        expected_evidence_keywords=list(data.get("expectedEvidenceKeywords") or []),
    )


def load_from_file(path: str) -> list[TestCase]:
    with Path(path).open("r", encoding="utf-8") as f:
        root = json.load(f)
    return [_parse_test_case(tc) for tc in root.get("testCases", [])]
