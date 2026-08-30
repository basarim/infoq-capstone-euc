package com.euc.grantfitassessment;

import com.euc.core.EucDefinition;
import com.euc.core.EucLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test of the EUC-driven run: EucLoader -> GrantFitApplication
 * -> PipelineBuilder -> ExecutionFilterRegistry -> implementations, using a
 * fake FitReasoner so it runs offline without LLM_API_KEY.
 *
 * This is the proof that execution is assembled from the EUC's declared
 * execution requirements rather than hand-wired, and that the halt contract
 * is honoured because the EUC says so. A live model call still needs a real
 * key, but the mechanics exercised here are exactly what a live call runs
 * through.
 */
class GrantFitApplicationTest {

    private final EucDefinition euc = EucLoader.loadGrantFitAssessment();

    private Organization strongFitOrg() {
        return new Organization(
                "Riverside Youth Coalition",
                "Providing after-school STEM programs to underserved youth.",
                List.of("STEM tutoring", "College readiness workshops"),
                "Pacific Northwest",
                true
        );
    }

    private GrantOpportunity standardGrant() {
        return new GrantOpportunity(
                "Evergreen Community Foundation",
                "Youth Education Grant",
                List.of("STEM education", "Underserved communities"),
                List.of("Registered 501(c)(3)", "Operating in Pacific Northwest"),
                List.of("Pacific Northwest"),
                true
        );
    }

    @Test
    void happyPath_eligibleOrgReachesReasonerAndReturnsItsClassification() {
        FakeFitReasoner reasoner = FakeFitReasoner.returning(
                "STRONG_FIT",
                "Mission and programs directly match funder priorities.",
                List.of("Strong STEM alignment", "Serves underserved youth"),
                List.of()
        );
        GrantFitApplication app = new GrantFitApplication(euc, reasoner);

        AssessmentResult result = app.assess(strongFitOrg(), standardGrant());

        assertTrue(result.eligible());
        assertTrue(result.failedEligibilityRules().isEmpty());
        assertEquals("STRONG_FIT", result.fitClassification());
        assertEquals(List.of("Strong STEM alignment", "Serves underserved youth"), result.supportingEvidence());
        assertTrue(reasoner.wasInvoked(), "expected ALIGNMENT-001 to run once eligibility passed");
    }

    @Test
    void ineligibleOrg_pipelineHaltsBeforeReasoningRuns() {
        FakeFitReasoner reasoner = FakeFitReasoner.thatMustNotBeInvoked();
        GrantFitApplication app = new GrantFitApplication(euc, reasoner);

        Organization notNonprofit = new Organization(
                "NextGen Learning Co.",
                "For-profit tutoring services for STEM subjects.",
                List.of("Paid STEM tutoring"),
                "Pacific Northwest",
                false
        );

        AssessmentResult result = app.assess(notNonprofit, standardGrant());

        assertFalse(result.eligible());
        assertEquals("POOR_FIT", result.fitClassification());
        assertTrue(result.failedEligibilityRules().stream().anyMatch(f -> f.startsWith("ELIGIBILITY-001")));
        assertFalse(reasoner.wasInvoked(), "onFailure: halt on ELIGIBILITY-001 should stop the pipeline before ALIGNMENT-001");
    }

    @Test
    void outOfRegionOrg_haltsAtGeographyStage() {
        FakeFitReasoner reasoner = FakeFitReasoner.thatMustNotBeInvoked();
        GrantFitApplication app = new GrantFitApplication(euc, reasoner);

        Organization outOfRegion = new Organization(
                "Coastal STEM Academy Trust",
                "Delivering STEM education to underserved youth on the East Coast.",
                List.of("STEM tutoring", "Robotics club"),
                "Northeast",
                true
        );

        AssessmentResult result = app.assess(outOfRegion, standardGrant());

        assertFalse(result.eligible());
        assertTrue(result.failedEligibilityRules().stream().anyMatch(f -> f.startsWith("GEOGRAPHY-001")));
        assertFalse(reasoner.wasInvoked());
    }
}
