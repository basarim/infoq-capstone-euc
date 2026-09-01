from __future__ import annotations

import os

from euc.core.context import PipelineContext
from euc.core.filters import ExecutionFilterRegistry
from euc.core.loader import load_grant_fit_assessment
from euc.core.models import EucDefinition
from euc.core.pipeline import PipelineBuilder
from euc.grantfitassessment import context_keys as keys
from euc.grantfitassessment.models import AssessmentResult, GrantOpportunity, Organization
from euc.grantfitassessment.pipeline.alignment_filter import AlignmentReasoningFilter
from euc.grantfitassessment.pipeline.eligibility_filter import eligibility_rule_filter
from euc.grantfitassessment.pipeline.geography_filter import geography_rule_filter
from euc.grantfitassessment.pipeline.required_info_filter import required_info_rule_filter
from euc.grantfitassessment.reasoner import FitReasoner, LlmFitReasoner


class GrantFitApplication:
    def __init__(self, euc: EucDefinition, fit_reasoner: FitReasoner) -> None:
        registry = ExecutionFilterRegistry()
        registry.register("ELIGIBILITY-001", eligibility_rule_filter)
        registry.register("GEOGRAPHY-001", geography_rule_filter)
        registry.register("INFO-001", required_info_rule_filter)
        registry.register("ALIGNMENT-001", AlignmentReasoningFilter(fit_reasoner, euc))
        self._pipeline_builder = PipelineBuilder(euc, registry)

    def assess(self, org: Organization, grant: GrantOpportunity) -> AssessmentResult:
        context = PipelineContext()
        context.put(keys.ORGANIZATION, org)
        context.put(keys.GRANT, grant)

        self._pipeline_builder.run(context)

        eligible: bool = context.get(keys.ELIGIBLE)
        if not eligible:
            failed_rules = context.get(keys.FAILED_ELIGIBILITY_RULES)
            return AssessmentResult(
                eligible=False,
                failed_eligibility_rules=failed_rules,
                fit_classification="POOR_FIT",
                explanation="Organization does not meet mandatory eligibility requirements.",
                supporting_evidence=[],
                identified_uncertainty=[],
            )

        return AssessmentResult(
            eligible=True,
            failed_eligibility_rules=[],
            fit_classification=context.get(keys.FIT_CLASSIFICATION),
            explanation=context.get(keys.EXPLANATION),
            supporting_evidence=context.get(keys.SUPPORTING_EVIDENCE),
            identified_uncertainty=context.get(keys.IDENTIFIED_UNCERTAINTY),
        )


def main() -> None:
    euc = load_grant_fit_assessment()
    print(f"Loaded EUC: {euc.id} — goal: {euc.goal}")

    model_name = os.environ.get("LLM_MODEL", "claude-sonnet-4-6")
    app = GrantFitApplication(euc, LlmFitReasoner(model_name))

    org = Organization(
        name="Riverside Youth Coalition",
        mission_statement="Providing after-school STEM programs to underserved youth.",
        programs=["STEM tutoring", "College readiness workshops"],
        operating_region="Pacific Northwest",
        is_registered_nonprofit=True,
    )
    grant = GrantOpportunity(
        funder_name="Evergreen Community Foundation",
        grant_name="Youth Education Grant",
        funding_priorities=["STEM education", "Underserved communities"],
        eligibility_requirements=["Registered 501(c)(3)", "Operating in Pacific Northwest"],
        allowed_regions=["Pacific Northwest"],
        requires_registered_nonprofit=True,
    )

    result = app.assess(org, grant)
    print(result)


if __name__ == "__main__":
    main()
