package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.integration;

import org.springframework.stereotype.Component;

/** Deterministic stub: hashed bag-of-words vector. No model, no network, same input gives the same vector. */
@Component
public class HashedEmbeddingClient implements EmbeddingClient {

    static final int DIMENSIONS = 256;

    @Override
    public double[] embed(String text) {
        double[] v = new double[DIMENSIONS];
        for (String token : EmbeddingClient.tokens(text)) {
            v[Math.floorMod(token.hashCode(), DIMENSIONS)] += 1;
        }
        double norm = Math.sqrt(java.util.Arrays.stream(v).map(x -> x * x).sum());
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
        return v;
    }
}
