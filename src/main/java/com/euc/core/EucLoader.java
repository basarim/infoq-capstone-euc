package com.euc.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads an EucDefinition from a JSON resource on the classpath.
 *
 * This is intentionally the only place that reads the EUC file — both the
 * application (execution) and the evaluator (evaluation) call through this
 * loader, so they are guaranteed to be reading the same definition. See
 * docs/proposal.md Section 3: "the use case exists once, as a first-class
 * SDLC artifact."
 */
public class EucLoader {

    private final ObjectMapper mapper;

    public EucLoader() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public EucDefinition load(String classpathResource) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalArgumentException("EUC resource not found: " + classpathResource);
            }
            return mapper.readValue(in, EucDefinition.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load EUC from " + classpathResource, e);
        }
    }

    public static EucDefinition loadGrantFitAssessment() {
        return new EucLoader().load("euc/grant-fit-assessment.json");
    }
}
