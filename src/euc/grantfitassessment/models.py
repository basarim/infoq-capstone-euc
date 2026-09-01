from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Organization:
    name: str = ""
    mission_statement: str = ""
    programs: list[str] = field(default_factory=list)
    operating_region: str = ""
    is_registered_nonprofit: bool = False

    @classmethod
    def from_dict(cls, data: dict) -> "Organization":
        return cls(
            name=data.get("name", ""),
            mission_statement=data.get("missionStatement", ""),
            programs=list(data.get("programs", [])),
            operating_region=data.get("operatingRegion", ""),
            is_registered_nonprofit=bool(data.get("isRegisteredNonprofit", False)),
        )


@dataclass(frozen=True)
class GrantOpportunity:
    funder_name: str = ""
    grant_name: str = ""
    funding_priorities: list[str] = field(default_factory=list)
    eligibility_requirements: list[str] = field(default_factory=list)
    allowed_regions: list[str] = field(default_factory=list)
    requires_registered_nonprofit: bool = False

    @classmethod
    def from_dict(cls, data: dict) -> "GrantOpportunity":
        return cls(
            funder_name=data.get("funderName", ""),
            grant_name=data.get("grantName", ""),
            funding_priorities=list(data.get("fundingPriorities", [])),
            eligibility_requirements=list(data.get("eligibilityRequirements", [])),
            allowed_regions=list(data.get("allowedRegions", [])),
            requires_registered_nonprofit=bool(data.get("requiresRegisteredNonprofit", False)),
        )


@dataclass(frozen=True)
class AssessmentResult:
    eligible: bool
    failed_eligibility_rules: list[str]
    fit_classification: str
    explanation: str
    supporting_evidence: list[str]
    identified_uncertainty: list[str]
