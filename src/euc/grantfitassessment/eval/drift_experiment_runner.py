from __future__ import annotations

import math
from dataclasses import dataclass, field

from euc.core.models import EucDefinition
from euc.grantfitassessment.app import GrantFitApplication
from euc.grantfitassessment.eval.dataset import TestCase
from euc.grantfitassessment.eval.evaluator import EvaluationScore, GrantFitEvaluator
from euc.grantfitassessment.eval.fit_reasoner_variant import FitReasonerVariant
from euc.grantfitassessment.models import AssessmentResult


@dataclass(frozen=True)
class CaseOutcome:
    case_id: str
    actual: AssessmentResult
    score: EvaluationScore


@dataclass(frozen=True)
class VariantResult:
    variant: FitReasonerVariant
    flagged_case_ids: list[str]
    eligibility_correctness_drift_case_ids: list[str]
    evidence_grounding_rate: float
    outcomes: dict[str, CaseOutcome] = field(default_factory=dict)

    def any_drift_flagged(self) -> bool:
        return len(self.flagged_case_ids) > 0

    def deterministic_rule_stable(self) -> bool:
        return len(self.eligibility_correctness_drift_case_ids) == 0


class DriftExperimentRunner:
    def __init__(self, euc: EucDefinition, dataset: list[TestCase]) -> None:
        self._euc = euc
        self._dataset = dataset

    def run(self, baseline: FitReasonerVariant, candidates: list[FitReasonerVariant]):
        from euc.grantfitassessment.eval.drift_experiment_report import DriftExperimentReport

        baseline_outcomes = self._score_variant(baseline)
        variant_results = []
        for candidate in candidates:
            candidate_outcomes = self._score_variant(candidate)
            variant_results.append(self._compare(candidate, baseline_outcomes, candidate_outcomes))
        return DriftExperimentReport.from_results(baseline, variant_results)

    def _score_variant(self, variant: FitReasonerVariant) -> dict[str, CaseOutcome]:
        app = GrantFitApplication(self._euc, variant.reasoner)
        evaluator = GrantFitEvaluator(self._euc)

        outcomes: dict[str, CaseOutcome] = {}
        for test_case in self._dataset:
            actual = app.assess(test_case.organization, test_case.grant)
            score = evaluator.evaluate(test_case, actual)
            outcomes[test_case.case_id] = CaseOutcome(test_case.case_id, actual, score)
        return outcomes

    def _compare(
        self,
        candidate: FitReasonerVariant,
        baseline_outcomes: dict[str, CaseOutcome],
        candidate_outcomes: dict[str, CaseOutcome],
    ) -> VariantResult:
        flagged_cases: list[str] = []
        eligibility_correctness_drift_cases: list[str] = []
        eligible_cases = 0
        grounded_eligible_cases = 0

        for case_id in baseline_outcomes:
            baseline_outcome = baseline_outcomes[case_id]
            candidate_outcome = candidate_outcomes[case_id]

            if baseline_outcome.score.all_passed() and not candidate_outcome.score.all_passed():
                flagged_cases.append(case_id)

            if baseline_outcome.score.eligibility_correctness != candidate_outcome.score.eligibility_correctness:
                eligibility_correctness_drift_cases.append(case_id)

            if candidate_outcome.actual.eligible:
                eligible_cases += 1
                if candidate_outcome.score.evidence_grounding:
                    grounded_eligible_cases += 1

        evidence_grounding_rate = (
            math.nan if eligible_cases == 0 else grounded_eligible_cases / eligible_cases
        )

        return VariantResult(
            variant=candidate,
            flagged_case_ids=flagged_cases,
            eligibility_correctness_drift_case_ids=eligibility_correctness_drift_cases,
            evidence_grounding_rate=evidence_grounding_rate,
            outcomes=candidate_outcomes,
        )
