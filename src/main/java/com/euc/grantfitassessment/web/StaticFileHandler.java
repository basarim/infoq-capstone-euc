package com.euc.grantfitassessment.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serves static files for the experimentation UI from classpath resources
 * under web/ (src/main/resources/web/). Deliberately minimal: no directory
 * listing, no caching headers, no compression — this is a local
 * experimentation tool, not a production static file server.
 */
class StaticFileHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonHttpUtil.sendMethodNotAllowed(exchange, "GET");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }
        // Classpath resource lookup below only ever reads under web/, but
        // strip ".." defensively so a crafted path can't walk outside it.
        String resourcePath = "web" + path.replace("..", "");

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] bytes = in.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentTypeFor(resourcePath));
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    private static String contentTypeFor(String path) {
        if (path.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (path.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (path.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        return "application/octet-stream";
    }
}
