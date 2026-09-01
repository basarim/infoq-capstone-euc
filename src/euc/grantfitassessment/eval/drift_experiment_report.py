from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Callable

from euc.grantfitassessment.eval.drift_experiment_runner import VariantResult
from euc.grantfitassessment.eval.fit_reasoner_variant import FitReasonerVariant


def _java_bool(value: bool) -> str:
    return "true" if value else "false"


def _java_list_str(items: list[str]) -> str:
    return "[" + ", ".join(items) + "]"


def _pct(rate: float) -> str:
    if math.isnan(rate):
        return "n/a (no variants in this category)"
    return f"{rate * 100:.1f}%"


def _rate_of(results: list, predicate: Callable[[object], bool]) -> float:
    if not results:
        return math.nan
    return sum(1 for r in results if predicate(r)) / len(results)


def _average_of(results: list, to_value: Callable[[object], float]) -> float:
    values = [v for v in (to_value(r) for r in results) if not math.isnan(v)]
    if not values:
        return math.nan
    return sum(values) / len(values)


@dataclass(frozen=True)
class DriftExperimentReport:
    baseline: FitReasonerVariant
    variant_results: list[VariantResult]
    drift_detection_rate: float
    false_flag_rate: float
    deterministic_rule_stability_rate: float
    evidence_grounding_consistency_rate: float

    @classmethod
    def from_results(cls, baseline: FitReasonerVariant, variant_results: list[VariantResult]) -> "DriftExperimentReport":
        altering_variants = [vr for vr in variant_results if vr.variant.expected_to_alter_behavior]
        neutral_variants = [vr for vr in variant_results if not vr.variant.expected_to_alter_behavior]

        return cls(
            baseline=baseline,
            variant_results=variant_results,
            drift_detection_rate=_rate_of(altering_variants, VariantResult.any_drift_flagged),
            false_flag_rate=_rate_of(neutral_variants, VariantResult.any_drift_flagged),
            deterministic_rule_stability_rate=_rate_of(variant_results, VariantResult.deterministic_rule_stable),
            evidence_grounding_consistency_rate=_average_of(
                variant_results, lambda vr: vr.evidence_grounding_rate
            ),
        )

    def summary(self) -> str:
        lines = [
            f"Drift experiment — baseline: {self.baseline.label}",
            f"  drift-detection-rate:              {_pct(self.drift_detection_rate)}",
            f"  false-flag-rate:                   {_pct(self.false_flag_rate)}",
            f"  deterministic-rule-stability-rate: {_pct(self.deterministic_rule_stability_rate)}",
            f"  evidence-grounding-consistency:    {_pct(self.evidence_grounding_consistency_rate)}",
            "",
            "Per-variant:",
        ]
        for vr in self.variant_results:
            lines.append(
                f"  {vr.variant.label:<40} "
                f"expectedToAlterBehavior={_java_bool(vr.variant.expected_to_alter_behavior):<5} "
                f"flagged={_java_list_str(vr.flagged_case_ids)} "
                f"stable={_java_bool(vr.deterministic_rule_stable())} "
                f"groundingRate={_pct(vr.evidence_grounding_rate)}"
            )
        return "\n".join(lines)
