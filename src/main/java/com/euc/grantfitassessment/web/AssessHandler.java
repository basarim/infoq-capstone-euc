package com.euc.grantfitassessment.web;

import com.euc.grantfitassessment.AssessmentResult;
import com.euc.grantfitassessment.GrantFitApplication;
import com.euc.grantfitassessment.GrantOpportunity;
import com.euc.grantfitassessment.Organization;
import com.euc.grantfitassessment.json.GrantFitJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * POST /api/assess — body: {"organization": {...}, "grant": {...}}, runs
 * the EUC-driven pipeline (GrantFitApplication.assess) and returns the
 * resulting AssessmentResult. This is the endpoint that makes a live call
 * to the Anthropic API (via AlignmentReasoningFilter -> LlmFitReasoner),
 * so it needs LLM_API_KEY set in the server process's environment.
 */
class AssessHandler implements HttpHandler {

    private final GrantFitApplication app;

    AssessHandler(GrantFitApplication app) {
        this.app = app;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonHttpUtil.sendMethodNotAllowed(exchange, "POST");
            return;
        }

        Organization org;
        GrantOpportunity grant;
        try {
            JsonNode body = JsonHttpUtil.readJsonBody(exchange);
            org = GrantFitJson.parseOrganization(requireField(body, "organization"));
            grant = GrantFitJson.parseGrant(requireField(body, "grant"));
        } catch (Exception e) {
            JsonHttpUtil.sendError(exchange, 400, "Malformed request body: " + e.getMessage());
            return;
        }

        try {
            AssessmentResult result = app.assess(org, grant);
            JsonHttpUtil.sendJson(exchange, 200, result);
        } catch (RuntimeException e) {
            // LLM_API_KEY missing/invalid, non-200 from Anthropic, or a
            // malformed model response -- all surfaced as an upstream
            // failure rather than a 500, so the UI can show a clear message.
            JsonHttpUtil.sendError(exchange, 502, e.getMessage());
        }
    }

    private static JsonNode requireField(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("missing required field '" + field + "'");
        }
        return node;
    }
}
