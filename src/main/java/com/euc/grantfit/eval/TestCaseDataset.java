package com.euc.grantfit.eval;

import com.euc.grantfit.GrantOpportunity;
import com.euc.grantfit.Organization;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads TestCase entries from eval/dataset/test-cases.json.
 *
 * Reads via a JsonNode tree rather than direct record binding because
 * Organization/GrantOpportunity/TestCase are records with more fields
 * than any single JSON shape maps 1:1 — explicit field reads keep the
 * dataset format decoupled from Java record constructor order.
 */
public class TestCaseDataset {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<TestCase> loadFromClasspath(String classpathResource) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalArgumentException("Dataset resource not found: " + classpathResource);
            }
            return parse(mapper.readTree(in));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load eval dataset from " + classpathResource, e);
        }
    }

    public List<TestCase> loadFromFile(java.io.File file) {
        try {
            return parse(mapper.readTree(file));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load eval dataset from " + file, e);
        }
    }

    private List<TestCase> parse(JsonNode root) {
        List<TestCase> result = new ArrayList<>();
        for (JsonNode node : root.get("testCases")) {
            result.add(parseTestCase(node));
        }
        return result;
    }

    private TestCase parseTestCase(JsonNode node) {
        Organization org = parseOrganization(node.get("organization"));
        GrantOpportunity grant = parseGrant(node.get("grant"));

        return new TestCase(
                node.get("caseId").asText(),
                org,
                grant,
                node.get("expectedEligible").asBoolean(),
                node.get("expectedFitClassification").asText(),
                node.get("groundTruthRationale").asText(),
                toStringList(node.get("expectedEvidenceKeywords"))
        );
    }

    private Organization parseOrganization(JsonNode n) {
        return new Organization(
                n.get("name").asText(),
                n.get("missionStatement").asText(),
                toStringList(n.get("programs")),
                n.get("operatingRegion").asText(),
                n.get("isRegisteredNonprofit").asBoolean()
        );
    }

    private GrantOpportunity parseGrant(JsonNode n) {
        return new GrantOpportunity(
                n.get("funderName").asText(),
                n.get("grantName").asText(),
                toStringList(n.get("fundingPriorities")),
                toStringList(n.get("eligibilityRequirements")),
                toStringList(n.get("allowedRegions")),
                n.get("requiresRegisteredNonprofit").asBoolean()
        );
    }

    private List<String> toStringList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null) {
            arrayNode.forEach(n -> list.add(n.asText()));
        }
        return list;
    }
}
