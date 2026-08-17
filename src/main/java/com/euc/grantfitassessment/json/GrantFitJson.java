package com.euc.grantfitassessment.json;

import com.euc.grantfitassessment.GrantOpportunity;
import com.euc.grantfitassessment.Organization;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared JSON <-> record conversion for Organization/GrantOpportunity, used
 * by both TestCaseDataset (reading eval/grant-fit-assessment/dataset) and
 * the REST API's request/response bodies (com.euc.grantfitassessment.web).
 * Reads via a JsonNode tree rather than direct record binding to keep the
 * external JSON shape decoupled from Java record constructor order.
 */
public final class GrantFitJson {

    private GrantFitJson() {
    }

    public static Organization parseOrganization(JsonNode n) {
        return new Organization(
                n.get("name").asText(),
                n.get("missionStatement").asText(),
                toStringList(n.get("programs")),
                n.get("operatingRegion").asText(),
                n.get("isRegisteredNonprofit").asBoolean()
        );
    }

    public static GrantOpportunity parseGrant(JsonNode n) {
        return new GrantOpportunity(
                n.get("funderName").asText(),
                n.get("grantName").asText(),
                toStringList(n.get("fundingPriorities")),
                toStringList(n.get("eligibilityRequirements")),
                toStringList(n.get("allowedRegions")),
                n.get("requiresRegisteredNonprofit").asBoolean()
        );
    }

    public static List<String> toStringList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null) {
            arrayNode.forEach(n -> list.add(n.asText()));
        }
        return list;
    }
}
