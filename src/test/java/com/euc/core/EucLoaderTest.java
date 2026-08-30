package com.euc.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the EUC as a business artifact: that it loads, that its parts are
 * declared, and above all that the link between evaluation and business
 * intent actually holds.
 *
 * The traceability tests are the ones that matter. An EUC whose criteria
 * point at requirements it does not declare is broken in exactly the way
 * this project exists to prevent, so that is a load-time failure rather
 * than something discovered later as a result nobody can explain.
 */
class EucLoaderTest {

    @Test
    void loadsGrantFitAssessmentEuc() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        assertEquals("grant-fit-assessment", euc.getId());
        assertEquals("Nonprofit Program Manager", euc.getActor());
        assertNotNull(euc.getGoal());
        assertEquals(3, euc.getExpectedOutcomes().size());
        assertTrue(euc.getExpectedOutcomes().contains("STRONG_FIT"));
    }

    @Test
    void declaresRulesAndPoliciesWithIds() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        assertFalse(euc.getRules().isEmpty(), "expected declared business rules");
        assertFalse(euc.getPolicies().isEmpty(), "expected declared policies");

        for (BusinessRule rule : euc.getRules()) {
            assertNotNull(rule.getId(), "every rule needs an id so a criterion can trace to it");
            assertNotNull(rule.getDescription());
        }
        for (Policy policy : euc.getPolicies()) {
            assertNotNull(policy.getId(), "every policy needs an id so a criterion can trace to it");
            assertNotNull(policy.getDescription());
        }
    }

    @Test
    void separatesDeterministicAndReasonedRequirements() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        assertTrue(euc.deterministicRequirements().size() >= 3,
                "expected multiple deterministic requirements");
        assertEquals(1, euc.reasonedRequirements().size(),
                "expected exactly one reasoned requirement (ALIGNMENT-001)");
    }

    @Test
    void deterministicRequirementsHaltOnFailure() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        for (ExecutionRequirement requirement : euc.deterministicRequirements()) {
            assertEquals(ExecutionRequirement.OnFailure.HALT, requirement.getOnFailure(),
                    "a failed mandatory requirement must stop the run: " + requirement.getId());
        }
    }

    @Test
    void everyCriterionTracesToSomethingTheEucDeclares() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        for (EvaluationCriterion criterion : euc.getEvaluationCriteria()) {
            assertFalse(criterion.getTracesTo().isEmpty(),
                    "criterion " + criterion.getId() + " traces to nothing");
            for (String target : criterion.getTracesTo()) {
                assertTrue(euc.traceableIds().contains(target),
                        "criterion " + criterion.getId() + " traces to unknown id " + target);
            }
        }
    }

    @Test
    void criteriaCoverEveryRequirementRuleAndPolicy() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        assertEquals(List.of(), euc.untracedIds(),
                "nothing this EUC declares should go unchecked — see docs/proposal.md, 'Visible mapping gaps'");
    }

    @Test
    void everyCriterionHasAtLeastOneStatedCheck() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        for (EvaluationCriterion criterion : euc.getEvaluationCriteria()) {
            assertFalse(criterion.getCriteria().isEmpty(),
                    "criterion " + criterion.getId() + " states nothing to check");
        }
    }

    @Test
    void contextIsDeclaredWithSeedFields() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        assertNotNull(euc.getContext(), "expected a declared shared-context spec");
        assertTrue(euc.getContext().getSeedFields().containsAll(List.of("organization", "grant")));
    }

    @Test
    void laterRequirementReadsWhatAnEarlierOneWrites() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        ExecutionRequirement alignment = euc.findRequirement("ALIGNMENT-001");
        assertTrue(alignment.getReads().contains("eligible"));

        int alignmentIndex = euc.getExecutionRequirements().indexOf(alignment);
        boolean earlierRequirementWritesEligible = euc.getExecutionRequirements().stream()
                .limit(alignmentIndex)
                .anyMatch(r -> r.getWrites() != null && r.getWrites().contains("eligible"));

        assertTrue(earlierRequirementWritesEligible,
                "ALIGNMENT-001 reads 'eligible', so an earlier requirement must write it");
    }

    @Test
    void criteriaReadFieldsExecutionWrote() {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        for (EvaluationCriterion criterion : euc.getEvaluationCriteria()) {
            if (criterion.getReads() == null) {
                continue;
            }
            for (String field : criterion.getReads()) {
                boolean written = euc.getExecutionRequirements().stream()
                        .anyMatch(r -> r.getWrites() != null && r.getWrites().contains(field));
                assertTrue(written,
                        "criterion " + criterion.getId() + " reads '" + field
                                + "', which no execution requirement writes");
            }
        }
    }

    @Test
    void loadedEucPassesValidation() {
        assertDoesNotThrow(EucLoader::loadGrantFitAssessment);
    }

    // ---- validation failures -------------------------------------------------

    @Test
    void emptyExecutionRequirementsFailsValidation() {
        EucDefinition euc = minimalValidEuc();
        euc.setExecutionRequirements(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, euc::validate);
        assertTrue(ex.getMessage().contains("executionRequirements"));
    }

    @Test
    void emptyEvaluationCriteriaFailsValidation() {
        EucDefinition euc = minimalValidEuc();
        euc.setEvaluationCriteria(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, euc::validate);
        assertTrue(ex.getMessage().contains("evaluationCriteria"));
    }

    @Test
    void criterionTracingToNothingFailsValidation() {
        EucDefinition euc = minimalValidEuc();
        euc.getEvaluationCriteria().get(0).setTracesTo(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, euc::validate);
        assertTrue(ex.getMessage().contains("traces to nothing"));
    }

    @Test
    void criterionTracingToUnknownIdFailsValidation() {
        EucDefinition euc = minimalValidEuc();
        euc.getEvaluationCriteria().get(0).setTracesTo(List.of("REQUIREMENT-THAT-DOES-NOT-EXIST"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, euc::validate);
        assertTrue(ex.getMessage().contains("REQUIREMENT-THAT-DOES-NOT-EXIST"),
                "the message should name the broken link: " + ex.getMessage());
    }

    @Test
    void requirementWithoutTypeFailsValidation() {
        EucDefinition euc = minimalValidEuc();
        euc.getExecutionRequirements().get(0).setType(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, euc::validate);
        assertTrue(ex.getMessage().contains("no type"));
    }

    @Test
    void untracedIdsReportsWhatNoCriterionChecks() {
        EucDefinition euc = minimalValidEuc();

        Policy unchecked = new Policy();
        unchecked.setId("POLICY-NOBODY-CHECKS");
        unchecked.setDescription("A constraint with no evaluator behind it");
        euc.setPolicies(List.of(unchecked));

        // Still valid — an unmeasured requirement is a finding to report, not a
        // malformed artifact — but it must be visible.
        assertDoesNotThrow(euc::validate);
        assertTrue(euc.untracedIds().contains("POLICY-NOBODY-CHECKS"));
    }

    /** A structurally valid EUC, built in code, for exercising validate() failures. */
    private EucDefinition minimalValidEuc() {
        EucDefinition euc = new EucDefinition();
        euc.setId("test-euc");

        ExecutionRequirement requirement = new ExecutionRequirement();
        requirement.setId("REQ-001");
        requirement.setType(ExecutionRequirement.Type.DETERMINISTIC);
        requirement.setResponsibility("Do the thing the business requires");
        euc.setExecutionRequirements(new java.util.ArrayList<>(List.of(requirement)));

        EvaluationCriterion criterion = new EvaluationCriterion();
        criterion.setId("EVAL-001");
        criterion.setTracesTo(new java.util.ArrayList<>(List.of("REQ-001")));
        criterion.setCriteria(new java.util.ArrayList<>(List.of("The thing was done")));
        euc.setEvaluationCriteria(new java.util.ArrayList<>(List.of(criterion)));

        return euc;
    }
}
