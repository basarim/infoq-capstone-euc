"""Tests the EUC as a business artifact: that it loads, that its parts are
declared, and above all that the link between evaluation and business intent
actually holds.

The traceability tests are the ones that matter. An EUC whose criteria point
at requirements it does not declare is broken in exactly the way this project
exists to prevent, so that is a load-time failure rather than something
discovered later as a result nobody can explain.
"""

from __future__ import annotations

import pytest

from euc.core.loader import load_grant_fit_assessment
from euc.core.models import EucDefinition, EvaluationCriterion, ExecutionRequirement, ExecutionRequirementType, OnFailure, Policy


def test_loads_grant_fit_assessment_euc():
    euc = load_grant_fit_assessment()

    assert euc.id == "grant-fit-assessment"
    assert euc.actor == "Nonprofit Program Manager"
    assert euc.goal is not None
    assert len(euc.expected_outcomes) == 3
    assert "STRONG_FIT" in euc.expected_outcomes


def test_declares_rules_and_policies_with_ids():
    euc = load_grant_fit_assessment()

    assert euc.rules, "expected declared business rules"
    assert euc.policies, "expected declared policies"

    for rule in euc.rules:
        assert rule.id is not None, "every rule needs an id so a criterion can trace to it"
        assert rule.description is not None
    for policy in euc.policies:
        assert policy.id is not None, "every policy needs an id so a criterion can trace to it"
        assert policy.description is not None


def test_separates_deterministic_and_reasoned_requirements():
    euc = load_grant_fit_assessment()

    assert len(euc.deterministic_requirements()) >= 3, "expected multiple deterministic requirements"
    assert len(euc.reasoned_requirements()) == 1, "expected exactly one reasoned requirement (ALIGNMENT-001)"


def test_deterministic_requirements_halt_on_failure():
    euc = load_grant_fit_assessment()

    for requirement in euc.deterministic_requirements():
        assert requirement.on_failure == OnFailure.HALT, (
            f"a failed mandatory requirement must stop the run: {requirement.id}"
        )


def test_every_criterion_traces_to_something_the_euc_declares():
    euc = load_grant_fit_assessment()

    for criterion in euc.evaluation_criteria:
        assert criterion.traces_to, f"criterion {criterion.id} traces to nothing"
        for target in criterion.traces_to:
            assert target in euc.traceable_ids(), f"criterion {criterion.id} traces to unknown id {target}"


def test_criteria_cover_every_requirement_rule_and_policy():
    euc = load_grant_fit_assessment()

    assert euc.untraced_ids() == [], (
        "nothing this EUC declares should go unchecked — see docs/proposal.md, 'Visible mapping gaps'"
    )


def test_every_criterion_has_at_least_one_stated_check():
    euc = load_grant_fit_assessment()

    for criterion in euc.evaluation_criteria:
        assert criterion.criteria, f"criterion {criterion.id} states nothing to check"


def test_context_is_declared_with_seed_fields():
    euc = load_grant_fit_assessment()

    assert euc.context is not None, "expected a declared shared-context spec"
    assert {"organization", "grant"}.issubset(set(euc.context.seed_fields))


def test_later_requirement_reads_what_an_earlier_one_writes():
    euc = load_grant_fit_assessment()

    alignment = euc.find_requirement("ALIGNMENT-001")
    assert "eligible" in alignment.reads

    alignment_index = euc.execution_requirements.index(alignment)
    earlier_requirement_writes_eligible = any(
        "eligible" in (r.writes or []) for r in euc.execution_requirements[:alignment_index]
    )

    assert earlier_requirement_writes_eligible, (
        "ALIGNMENT-001 reads 'eligible', so an earlier requirement must write it"
    )


def test_criteria_read_fields_execution_wrote():
    euc = load_grant_fit_assessment()

    for criterion in euc.evaluation_criteria:
        for field_name in criterion.reads or []:
            written = any(field_name in (r.writes or []) for r in euc.execution_requirements)
            assert written, f"criterion {criterion.id} reads '{field_name}', which no execution requirement writes"


def test_loaded_euc_passes_validation():
    load_grant_fit_assessment()  # raises on failure


# ---- validation failures --------------------------------------------------


def _minimal_valid_euc() -> EucDefinition:
    """A structurally valid EUC, built in code, for exercising validate() failures."""
    requirement = ExecutionRequirement(
        id="REQ-001",
        type=ExecutionRequirementType.DETERMINISTIC,
        responsibility="Do the thing the business requires",
    )
    criterion = EvaluationCriterion(
        id="EVAL-001",
        traces_to=["REQ-001"],
        criteria=["The thing was done"],
    )
    return EucDefinition(
        id="test-euc",
        execution_requirements=[requirement],
        evaluation_criteria=[criterion],
    )


def test_empty_execution_requirements_fails_validation():
    euc = _minimal_valid_euc()
    euc.execution_requirements = []

    with pytest.raises(ValueError, match="executionRequirements"):
        euc.validate()


def test_empty_evaluation_criteria_fails_validation():
    euc = _minimal_valid_euc()
    euc.evaluation_criteria = []

    with pytest.raises(ValueError, match="evaluationCriteria"):
        euc.validate()


def test_criterion_tracing_to_nothing_fails_validation():
    euc = _minimal_valid_euc()
    euc.evaluation_criteria[0].traces_to = []

    with pytest.raises(ValueError, match="traces to nothing"):
        euc.validate()


def test_criterion_tracing_to_unknown_id_fails_validation():
    euc = _minimal_valid_euc()
    euc.evaluation_criteria[0].traces_to = ["REQUIREMENT-THAT-DOES-NOT-EXIST"]

    with pytest.raises(ValueError, match="REQUIREMENT-THAT-DOES-NOT-EXIST"):
        euc.validate()


def test_requirement_without_type_fails_validation():
    euc = _minimal_valid_euc()
    euc.execution_requirements[0].type = None

    with pytest.raises(ValueError, match="no type"):
        euc.validate()


def test_untraced_ids_reports_what_no_criterion_checks():
    euc = _minimal_valid_euc()
    euc.policies = [Policy(id="POLICY-NOBODY-CHECKS", description="A constraint with no evaluator behind it")]

    # Still valid — an unmeasured requirement is a finding to report, not a
    # malformed artifact — but it must be visible.
    euc.validate()
    assert "POLICY-NOBODY-CHECKS" in euc.untraced_ids()
