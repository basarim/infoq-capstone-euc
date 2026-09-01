from __future__ import annotations

import json
import math
from pathlib import Path

from euc.grantfitassessment.eval.drift_experiment_report import DriftExperimentReport


def _nan_safe(value: float) -> float | None:
    return None if math.isnan(value) else value


def _to_dict(report: DriftExperimentReport) -> dict:
    variants = []
    for vr in report.variant_results:
        cases = {}
        for case_id, outcome in vr.outcomes.items():
            cases[case_id] = {
                "eligible": outcome.actual.eligible,
                "fitClassification": outcome.actual.fit_classification,
                "eligibilityCorrectness": outcome.score.eligibility_correctness,
                "programAlignment": outcome.score.program_alignment,
                "evidenceGrounding": outcome.score.evidence_grounding,
                "allPassed": outcome.score.all_passed(),
            }
        variants.append(
            {
                "label": vr.variant.label,
                "expectedToAlterBehavior": vr.variant.expected_to_alter_behavior,
                "anyDriftFlagged": vr.any_drift_flagged(),
                "deterministicRuleStable": vr.deterministic_rule_stable(),
                "evidenceGroundingRate": _nan_safe(vr.evidence_grounding_rate),
                "flaggedCaseIds": vr.flagged_case_ids,
                "eligibilityCorrectnessDriftCaseIds": vr.eligibility_correctness_drift_case_ids,
                "cases": cases,
            }
        )

    return {
        "baseline": report.baseline.label,
        "driftDetectionRate": _nan_safe(report.drift_detection_rate),
        "falseFlagRate": _nan_safe(report.false_flag_rate),
        "deterministicRuleStabilityRate": _nan_safe(report.deterministic_rule_stability_rate),
        "evidenceGroundingConsistencyRate": _nan_safe(report.evidence_grounding_consistency_rate),
        "variants": variants,
    }


def write_json(report: DriftExperimentReport, output_path: str) -> None:
    path = Path(output_path)
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8") as f:
            json.dump(_to_dict(report), f, indent=2)
    except OSError as e:
        raise RuntimeError(f"Failed to write drift experiment report to {output_path}") from e
