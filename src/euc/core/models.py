"""The EUC's data model: goal, rules, policies, execution requirements,
evaluation criteria, and the structural contract that binds them together.

This is a domain-agnostic model — nothing here knows about grants, nonprofits,
or any other business domain. A single EUC instance describes one bounded
business capability.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum


@dataclass
class BusinessRule:
    id: str = ""
    description: str = ""

    @classmethod
    def from_dict(cls, data: dict) -> "BusinessRule":
        return cls(id=data.get("id", ""), description=data.get("description", ""))


@dataclass
class Policy:
    id: str = ""
    description: str = ""

    @classmethod
    def from_dict(cls, data: dict) -> "Policy":
        return cls(id=data.get("id", ""), description=data.get("description", ""))


@dataclass
class EucContext:
    description: str = ""
    seed_fields: list[str] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: dict) -> "EucContext":
        return cls(
            description=data.get("description", ""),
            seed_fields=list(data.get("seedFields", [])),
        )


@dataclass
class EvaluationCriterion:
    """`tracesTo` is the keystone of the whole artifact: it is the only thing
    that makes traceability concrete rather than aspirational."""

    id: str = ""
    traces_to: list[str] = field(default_factory=list)
    criteria: list[str] = field(default_factory=list)
    reads: list[str] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: dict) -> "EvaluationCriterion":
        return cls(
            id=data.get("id", ""),
            traces_to=list(data.get("tracesTo", [])),
            criteria=list(data.get("criteria", [])),
            reads=list(data.get("reads", [])),
        )


class ExecutionRequirementType(str, Enum):
    DETERMINISTIC = "deterministic"
    REASONED = "reasoned"


class OnFailure(str, Enum):
    HALT = "halt"
    CONTINUE = "continue"


@dataclass
class ExecutionRequirement:
    id: str = ""
    type: ExecutionRequirementType | None = None
    responsibility: str = ""
    on_failure: OnFailure = OnFailure.CONTINUE
    reads: list[str] = field(default_factory=list)
    writes: list[str] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: dict) -> "ExecutionRequirement":
        raw_type = data.get("type")
        raw_on_failure = data.get("onFailure")
        return cls(
            id=data.get("id", ""),
            type=ExecutionRequirementType(raw_type) if raw_type else None,
            responsibility=data.get("responsibility", ""),
            on_failure=OnFailure(raw_on_failure) if raw_on_failure else OnFailure.CONTINUE,
            reads=list(data.get("reads", [])),
            writes=list(data.get("writes", [])),
        )


@dataclass
class EucDefinition:
    id: str = ""
    actor: str = ""
    goal: str = ""
    context: EucContext = field(default_factory=EucContext)
    rules: list[BusinessRule] = field(default_factory=list)
    policies: list[Policy] = field(default_factory=list)
    expected_outcomes: list[str] = field(default_factory=list)
    execution_requirements: list[ExecutionRequirement] = field(default_factory=list)
    evaluation_criteria: list[EvaluationCriterion] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: dict) -> "EucDefinition":
        return cls(
            id=data.get("id", ""),
            actor=data.get("actor", ""),
            goal=data.get("goal", ""),
            context=EucContext.from_dict(data.get("context", {}) or {}),
            rules=[BusinessRule.from_dict(r) for r in data.get("rules", [])],
            policies=[Policy.from_dict(p) for p in data.get("policies", [])],
            expected_outcomes=list(data.get("expectedOutcomes", [])),
            execution_requirements=[
                ExecutionRequirement.from_dict(r) for r in data.get("executionRequirements", [])
            ],
            evaluation_criteria=[
                EvaluationCriterion.from_dict(c) for c in data.get("evaluationCriteria", [])
            ],
        )

    def deterministic_requirements(self) -> list[ExecutionRequirement]:
        return [r for r in self.execution_requirements if r.type == ExecutionRequirementType.DETERMINISTIC]

    def reasoned_requirements(self) -> list[ExecutionRequirement]:
        return [r for r in self.execution_requirements if r.type == ExecutionRequirementType.REASONED]

    def find_requirement(self, requirement_id: str) -> ExecutionRequirement:
        for requirement in self.execution_requirements:
            if requirement.id == requirement_id:
                return requirement
        raise ValueError(f"No execution requirement with id {requirement_id}")

    def traceable_ids(self) -> set[str]:
        """Every id an evaluation criterion is allowed to trace to: execution
        requirements, rules, and policies — in insertion order."""
        ids: dict[str, None] = {}
        for requirement in self.execution_requirements:
            ids[requirement.id] = None
        for rule in self.rules:
            ids[rule.id] = None
        for policy in self.policies:
            ids[policy.id] = None
        return dict.fromkeys(ids)

    def untraced_ids(self) -> list[str]:
        """Declared ids (requirements, rules, policies) that no evaluation
        criterion traces to. Deliberately not enforced by validate(): some
        requirements may legitimately be unmeasurable, and hiding that would
        defeat the purpose. This is for callers to report, not to reject."""
        traced: dict[str, None] = {}
        for criterion in self.evaluation_criteria:
            for target in criterion.traces_to:
                traced[target] = None
        gaps = list(self.traceable_ids())
        return [i for i in gaps if i not in traced]

    def validate(self) -> None:
        """The structural contract, checked in this exact order."""
        if not self.execution_requirements:
            raise ValueError(
                f"EUC '{self.id}' is invalid: executionRequirements must declare one or more responsibilities"
            )
        if not self.evaluation_criteria:
            raise ValueError(
                f"EUC '{self.id}' is invalid: evaluationCriteria must declare one or more criteria"
            )

        for requirement in self.execution_requirements:
            if not requirement.id or not requirement.id.strip():
                raise ValueError(f"EUC '{self.id}' is invalid: an execution requirement has no id")
            if requirement.type is None:
                raise ValueError(
                    f"EUC '{self.id}' is invalid: execution requirement '{requirement.id}' has no type"
                )

        traceable = self.traceable_ids()

        for criterion in self.evaluation_criteria:
            if not criterion.id or not criterion.id.strip():
                raise ValueError(f"EUC '{self.id}' is invalid: an evaluation criterion has no id")
            if not criterion.traces_to:
                raise ValueError(
                    f"EUC '{self.id}' is invalid: evaluation criterion '{criterion.id}' traces to "
                    "nothing — a criterion that names no requirement cannot connect its result "
                    "back to business intent"
                )
            for target in criterion.traces_to:
                if target not in traceable:
                    raise ValueError(
                        f"EUC '{self.id}' is invalid: evaluation criterion '{criterion.id}' traces "
                        f"to '{target}', which is not a declared execution requirement, rule or policy"
                    )
