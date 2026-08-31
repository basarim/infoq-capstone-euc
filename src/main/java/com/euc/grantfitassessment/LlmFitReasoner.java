package com.euc.grantfitassessment;

import com.euc.core.EucDefinition;
import com.euc.core.Policy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM-backed implementation of FitReasoner, calling the Anthropic Messages
 * API directly.
 *
 * The model is asked to respond in strict JSON so the result can be parsed
 * without a separate structured-output framework. Per docs/proposal.md
 * Section 4, this is the implementation that gets swapped (model name,
 * prompt phrasing) during the drift experiments — the EUC and the rest of
 * the application do not change when this class does.
 */
public class LlmFitReasoner implements FitReasoner {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final String modelName;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmFitReasoner(String modelName) {
        this.modelName = modelName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public FitReasoning assessFit(Organization org, GrantOpportunity grant, EucDefinition euc) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY environment variable is not set. Required to call " + API_URL);
        }

        String prompt = buildPrompt(org, grant, euc);
        String responseBody = callApi(prompt, apiKey);
        return parseResponse(responseBody, euc);
    }

    protected String buildPrompt(Organization org, GrantOpportunity grant, EucDefinition euc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(euc.getGoal()).append("\n\n");
        sb.append("Policies:\n");
        for (Policy policy : euc.getPolicies()) {
            sb.append("- ").append(policy.getDescription()).append("\n");
        }
        sb.append("\nExpected outcomes (choose exactly one): ")
                .append(String.join(", ", euc.getExpectedOutcomes())).append("\n\n");

        sb.append("Organization:\n");
        sb.append("- Name: ").append(org.name()).append("\n");
        sb.append("- Mission: ").append(org.missionStatement()).append("\n");
        sb.append("- Programs: ").append(String.join(", ", org.programs())).append("\n\n");

        sb.append("Grant:\n");
        sb.append("- Funder: ").append(grant.funderName()).append("\n");
        sb.append("- Funding priorities: ")
                .append(String.join(", ", grant.fundingPriorities())).append("\n\n");

        sb.append(alignmentInstructions()).append("\n\n");

        sb.append("Respond with ONLY a JSON object, no other text, in exactly this shape:\n");
        sb.append("{\n")
                .append("  \"fitClassification\": \"STRONG_FIT|POSSIBLE_FIT|POOR_FIT\",\n")
                .append("  \"explanation\": \"...\",\n")
                .append("  \"supportingEvidence\": [\"...\"],\n")
                .append("  \"identifiedUncertainty\": [\"...\"]\n")
                .append("}");

        return sb.toString();
    }

    /**
     * The paragraph instructing the model how to weigh alignment evidence.
     * Overriding this (rather than all of buildPrompt) is the intended
     * extension point for a prompt-variant FitReasoner in the drift
     * experiment (docs/proposal.md Section 7, step 3: "edit the prompt's
     * alignment instructions") — see AlternateAlignmentPromptReasoner.
     */
    protected String alignmentInstructions() {
        return "Assess mission and program alignment. Cite specific evidence from the "
                + "organization's profile for your conclusion. If evidence is insufficient, "
                + "say so explicitly rather than guessing.";
    }

    private String callApi(String prompt, String apiKey) {
        try {
            ObjectNode payload = mapper.createObjectNode();
            payload.put("model", modelName);
            payload.put("max_tokens", 1024);

            ArrayNode messages = payload.putArray("messages");
            ObjectNode userMessage = mapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "LLM API call failed with status " + response.statusCode() + ": " + response.body());
            }

            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to call LLM API", e);
        }
    }

    private FitReasoning parseResponse(String responseBody, EucDefinition euc) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            String text = root.get("content").get(0).get("text").asText();

            // Model is instructed to return raw JSON, but strip code fences defensively.
            String cleaned = text.trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("```\\s*$", "")
                    .trim();

            JsonNode parsed = mapper.readTree(cleaned);

            String fitClassification = parsed.get("fitClassification").asText();
            if (!euc.getExpectedOutcomes().contains(fitClassification)) {
                throw new IllegalStateException(
                        "Model returned an outcome not in the EUC's expectedOutcomes: " + fitClassification);
            }

            return new FitReasoning(
                    fitClassification,
                    parsed.get("explanation").asText(),
                    toList(parsed.get("supportingEvidence")),
                    toList(parsed.get("identifiedUncertainty"))
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse LLM response: " + responseBody, e);
        }
    }

    private List<String> toList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null) {
            arrayNode.forEach(n -> list.add(n.asText()));
        }
        return list;
    }

    public String getModelName() {
        return modelName;
    }
}
