package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.integration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Port for the embedding model. Production: a hosted embedding model (for example Amazon Bedrock) plus pgvector. */
public interface EmbeddingClient {

    /** Returns an L2-normalised vector, so cosine similarity is a plain dot product. */
    double[] embed(String text);

    Set<String> STOP_WORDS = Set.of("the", "and", "for", "are", "how", "can", "what", "does", "you", "your",
            "with", "from", "that", "this", "have", "will", "not", "but", "was", "any", "into", "when", "which");

    /** Lowercase word tokens with stop words removed and a light suffix stem (refunds, refunded -> refund). */
    static List<String> tokens(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(t -> t.length() > 2 && !STOP_WORDS.contains(t))
                .map(EmbeddingClient::stem)
                .toList();
    }

    private static String stem(String t) {
        if (t.length() > 5 && t.endsWith("ing")) return t.substring(0, t.length() - 3);
        if (t.length() > 4 && t.endsWith("ed")) return t.substring(0, t.length() - 2);
        if (t.length() > 3 && t.endsWith("s") && !t.endsWith("ss")) return t.substring(0, t.length() - 1);
        return t;
    }
}
