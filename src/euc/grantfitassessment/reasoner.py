"""The reasoned half of the pipeline: a model call that weighs an
organization's mission and programs against a grant's funding priorities.

FitReasoner is the swap point — the pipeline filter that calls it doesn't
care whether it's backed by a live model, a fixture, or a different prompt.
"""

from __future__ import annotations

import json
import os
import re
from dataclasses import dataclass, field
from typing import Protocol

import requests

from euc.core.models import EucDefinition
from euc.grantfitassessment.models import GrantOpportunity, Organization

_API_URL = "https://api.anthropic.com/v1/messages"
_API_VERSION = "2023-06-01"


@dataclass(frozen=True)
class FitReasoning:
    fit_classification: str
    explanation: str
    supporting_evidence: list[str] = field(default_factory=list)
    identified_uncertainty: list[str] = field(default_factory=list)


class FitReasoner(Protocol):
    def assess_fit(self, org: Organization, grant: GrantOpportunity, euc: EucDefinition) -> FitReasoning: ...


class LlmFitReasoner:
    def __init__(self, model_name: str) -> None:
        self.model_name = model_name
        # Populated after each assess_fit() call — an observability side
        # channel (see langfuse_tracing.py), not part of this class's return
        # contract.
        self.last_usage: dict[str, int] | None = None

    def assess_fit(self, org: Organization, grant: GrantOpportunity, euc: EucDefinition) -> FitReasoning:
        api_key = os.environ.get("ANTHROPIC_API_KEY")
        if not api_key or not api_key.strip():
            raise RuntimeError(
                f"ANTHROPIC_API_KEY environment variable is not set. Required to call {_API_URL}"
            )
        prompt = self._build_prompt(org, grant, euc)
        response_body = self._call_api(prompt, api_key)
        return self._parse_response(response_body, euc)

    def _build_prompt(self, org: Organization, grant: GrantOpportunity, euc: EucDefinition) -> str:
        policies = "\n".join(f"- {p.description}" for p in euc.policies)
        outcomes = ", ".join(euc.expected_outcomes)
        programs = ", ".join(org.programs)
        priorities = ", ".join(grant.funding_priorities)

        return (
            f"Goal: {euc.goal}\n\n"
            f"Policies:\n{policies}\n\n"
            f"Expected outcomes (choose exactly one): {outcomes}\n\n"
            f"Organization:\n"
            f"- Name: {org.name}\n"
            f"- Mission: {org.mission_statement}\n"
            f"- Programs: {programs}\n\n"
            f"Grant:\n"
            f"- Funder: {grant.funder_name}\n"
            f"- Funding priorities: {priorities}\n\n"
            f"{self._alignment_instructions()}\n\n"
            "Respond with ONLY a JSON object, no other text, in exactly this shape:\n"
            "{\n"
            '  "fitClassification": "STRONG_FIT|POSSIBLE_FIT|POOR_FIT",\n'
            '  "explanation": "...",\n'
            '  "supportingEvidence": ["..."],\n'
            '  "identifiedUncertainty": ["..."]\n'
            "}"
        )

    def _alignment_instructions(self) -> str:
        return (
            "Assess mission and program alignment. Cite specific evidence from the "
            "organization's profile for your conclusion. If evidence is insufficient, "
            "say so explicitly rather than guessing."
        )

    def _call_api(self, prompt: str, api_key: str) -> str:
        headers = {
            "Content-Type": "application/json",
            "x-api-key": api_key,
            "anthropic-version": _API_VERSION,
        }
        workspace_id = os.environ.get("ANTHROPIC_WORKSPACE_ID")
        if workspace_id and workspace_id.strip():
            headers["anthropic-workspace-id"] = workspace_id

        body = {
            "model": self.model_name,
            "max_tokens": 1024,
            "messages": [{"role": "user", "content": prompt}],
        }

        try:
            response = requests.post(
                _API_URL, headers=headers, json=body, timeout=(20, 60)
            )
        except requests.RequestException as e:
            raise RuntimeError("Failed to call LLM API") from e

        if response.status_code != 200:
            raise RuntimeError(f"LLM API call failed with status {response.status_code}: {response.text}")

        return response.text

    def _parse_response(self, response_body: str, euc: EucDefinition) -> FitReasoning:
        try:
            root = json.loads(response_body)
            usage = root.get("usage") or {}
            if "input_tokens" in usage or "output_tokens" in usage:
                self.last_usage = {
                    "input": usage.get("input_tokens", 0),
                    "output": usage.get("output_tokens", 0),
                }
            text = root["content"][0]["text"]

            cleaned = text.strip()
            cleaned = re.sub(r"^```json\s*", "", cleaned)
            cleaned = re.sub(r"^```\s*", "", cleaned)
            cleaned = re.sub(r"```\s*$", "", cleaned)
            cleaned = cleaned.strip()

            parsed = json.loads(cleaned)
        except (json.JSONDecodeError, KeyError, IndexError, TypeError) as e:
            raise RuntimeError(f"Failed to parse LLM response: {response_body}") from e

        fit_classification = parsed.get("fitClassification")
        if fit_classification not in euc.expected_outcomes:
            raise RuntimeError(
                f"Model returned an outcome not in the EUC's expectedOutcomes: {fit_classification}"
            )

        return FitReasoning(
            fit_classification=fit_classification,
            explanation=parsed.get("explanation", ""),
            supporting_evidence=list(parsed.get("supportingEvidence") or []),
            identified_uncertainty=list(parsed.get("identifiedUncertainty") or []),
        )


class AlternateAlignmentPromptReasoner(LlmFitReasoner):
    """A 'loosened' variant that drops the cite-evidence / say-so-if-insufficient
    constraint — intended to be caught by the EVAL-EVIDENCE evaluation criterion
    in a controlled-change experiment."""

    def _alignment_instructions(self) -> str:
        return (
            "Assess mission and program alignment. Give your best judgment of fit "
            "even if the organization's profile doesn't fully spell out the connection."
        )
