"""Deterministic FitReasoner test double — no network call, no API key. Lets
tests exercise the full EUC-driven pipeline (GrantFitApplication ->
PipelineBuilder -> filters) without depending on a live model, and lets a
test assert the reasoner was never invoked when eligibility should have
halted the pipeline first."""

from __future__ import annotations

from euc.core.models import EucDefinition
from euc.grantfitassessment.models import GrantOpportunity, Organization
from euc.grantfitassessment.reasoner import FitReasoning


class FakeFitReasoner:
    def __init__(self, canned: FitReasoning | None) -> None:
        self._canned = canned
        self._invoked = False

    @classmethod
    def returning(
        cls,
        fit_classification: str,
        explanation: str,
        supporting_evidence: list[str],
        identified_uncertainty: list[str],
    ) -> "FakeFitReasoner":
        return cls(FitReasoning(fit_classification, explanation, supporting_evidence, identified_uncertainty))

    @classmethod
    def that_must_not_be_invoked(cls) -> "FakeFitReasoner":
        return cls(None)

    def assess_fit(self, org: Organization, grant: GrantOpportunity, euc: EucDefinition) -> FitReasoning:
        self._invoked = True
        if self._canned is None:
            raise AssertionError(
                "FitReasoner.assess_fit() was called but the pipeline should have halted "
                "before ALIGNMENT-001 ran (an earlier eligibility stage should have failed)"
            )
        return self._canned

    def was_invoked(self) -> bool:
        return self._invoked
