from __future__ import annotations

from dataclasses import dataclass

from euc.grantfitassessment.reasoner import FitReasoner


@dataclass(frozen=True)
class FitReasonerVariant:
    """A labeled configuration triple: display label, the FitReasoner
    implementation, and whether this variant is *expected* to change
    behavior — the ground truth the drift metrics are computed against."""

    label: str
    reasoner: FitReasoner
    expected_to_alter_behavior: bool
