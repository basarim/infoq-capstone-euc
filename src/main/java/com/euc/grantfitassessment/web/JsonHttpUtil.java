package com.euc.grantfitassessment.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Small shared helpers for the JDK-builtin HttpServer handlers in this
 * package: JSON request/response bodies, plus a consistent error-response
 * shape so the web UI can render a failure message instead of a blank
 * screen when e.g. LLM_API_KEY is missing or invalid.
 */
final class JsonHttpUtil {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonHttpUtil() {
    }

    static JsonNode readJsonBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return MAPPER.readTree(in);
        }
    }

    static void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /**
     * Sends a {"error": "..."} body. Status code convention used across
     * handlers in this package: 400 for a malformed request, 405 for the
     * wrong HTTP method, 502 for an upstream LLM call failure (missing/
     * invalid LLM_API_KEY, network error, non-200 from Anthropic), 500 for
     * anything else unexpected.
     */
    static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        ObjectNode error = MAPPER.createObjectNode();
        error.put("error", message);
        sendJson(exchange, statusCode, error);
    }

    static void sendMethodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        sendError(exchange, 405, "Method not allowed. Use " + allowed + ".");
    }

    static String readText(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
}
