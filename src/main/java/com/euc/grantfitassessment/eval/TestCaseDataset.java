package com.euc.grantfitassessment.eval;

import com.euc.grantfitassessment.GrantOpportunity;
import com.euc.grantfitassessment.Organization;
import com.euc.grantfitassessment.json.GrantFitJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads TestCase entries from eval/grant-fit-assessment/dataset/test-cases.json.
 *
 * Reads via a JsonNode tree rather than direct record binding because
 * Organization/GrantOpportunity/TestCase are records with more fields
 * than any single JSON shape maps 1:1 — explicit field reads keep the
 * dataset format decoupled from Java record constructor order. Organization/
 * GrantOpportunity parsing itself is shared with the REST API via GrantFitJson.
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
        Organization org = GrantFitJson.parseOrganization(node.get("organization"));
        GrantOpportunity grant = GrantFitJson.parseGrant(node.get("grant"));

        return new TestCase(
                node.get("caseId").asText(),
                org,
                grant,
                node.get("expectedEligible").asBoolean(),
                node.get("expectedFitClassification").asText(),
                node.get("groundTruthRationale").asText(),
                GrantFitJson.toStringList(node.get("expectedEvidenceKeywords"))
        );
    }
}
