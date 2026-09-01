"""Loads an EUC definition from JSON and enforces its structural contract.

Paths are plain filesystem paths relative to the current working directory —
the same convention the eval dataset already uses (`eval/.../test-cases.json`),
kept consistent here rather than mixing in a classpath-resource-style loader.
"""

from __future__ import annotations

import json
from pathlib import Path

from euc.core.models import EucDefinition

GRANT_FIT_ASSESSMENT_PATH = "resources/euc/grant-fit-assessment/grant-fit-assessment.json"


class EucLoader:
    def load(self, path: str) -> EucDefinition:
        resource = Path(path)
        if not resource.is_file():
            raise ValueError(f"EUC resource not found: {path}")
        try:
            with resource.open("r", encoding="utf-8") as f:
                data = json.load(f)
        except (OSError, json.JSONDecodeError) as e:
            raise RuntimeError(f"Failed to load EUC from {path}") from e

        euc = EucDefinition.from_dict(data)
        euc.validate()
        return euc


def load_grant_fit_assessment() -> EucDefinition:
    return EucLoader().load(GRANT_FIT_ASSESSMENT_PATH)
