package com.euc.grantfit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the deterministic layer only — no LLM call involved, so these
 * should stay green regardless of any prompt or model change (that
 * stability is the point per docs/proposal.md Section 4's secondary claim).
 */
class EligibilityCheckerTest {

    private final EligibilityChecker checker = new EligibilityChecker();

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
    void eligibleOrganizationPassesAllRules() {
        Organization org = new Organization(
                "Riverside Youth Coalition",
                "Providing after-school STEM programs to underserved youth.",
                List.of("STEM tutoring"),
                "Pacific Northwest",
                true
        );

        List<String> failures = checker.checkEligibility(org, standardGrant());
        assertTrue(failures.isEmpty(), "expected no eligibility failures, got: " + failures);
    }

    @Test
    void nonRegisteredNonprofitFailsEligibility001() {
        Organization org = new Organization(
                "NextGen Learning Co.",
                "For-profit tutoring services.",
                List.of("Paid STEM tutoring"),
                "Pacific Northwest",
                false
        );

        List<String> failures = checker.checkEligibility(org, standardGrant());
        assertTrue(failures.stream().anyMatch(f -> f.startsWith("ELIGIBILITY-001")));
    }

    @Test
    void outOfRegionOrganizationFailsGeography001() {
        Organization org = new Organization(
                "Coastal STEM Academy Trust",
                "Delivering STEM education to underserved youth.",
                List.of("STEM tutoring"),
                "Northeast",
                true
        );

        List<String> failures = checker.checkEligibility(org, standardGrant());
        assertTrue(failures.stream().anyMatch(f -> f.startsWith("GEOGRAPHY-001")));
    }

    @Test
    void missingMissionStatementFailsInfo001() {
        Organization org = new Organization(
                "Unnamed Org",
                "",
                List.of("Some program"),
                "Pacific Northwest",
                true
        );

        List<String> failures = checker.checkEligibility(org, standardGrant());
        assertTrue(failures.stream().anyMatch(f -> f.startsWith("INFO-001")));
    }

    @Test
    void missingProgramsFailsInfo001() {
        Organization org = new Organization(
                "Unnamed Org",
                "A mission statement.",
                List.of(),
                "Pacific Northwest",
                true
        );

        List<String> failures = checker.checkEligibility(org, standardGrant());
        assertTrue(failures.stream().anyMatch(f -> f.startsWith("INFO-001")));
    }

    @Test
    void multipleFailuresAreAllReported() {
        Organization org = new Organization(
                "Unnamed Org",
                "",
                List.of(),
                "Northeast",
                false
        );

        List<String> failures = checker.checkEligibility(org, standardGrant());
        assertEquals(4, failures.size(), "expected all four rule failures, got: " + failures);
    }
}
