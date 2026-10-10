package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.integration;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Deterministic stub "model": reads the prompt like an LLM would, picks the source sentence that best overlaps the
 * question and answers with it plus a [KB-id] citation. Says "I don't know." when nothing overlaps.
 */
@Component
public class ExtractiveLlmClient implements LlmClient {

    private static final Pattern SOURCE = Pattern.compile("^\\[KB-(\\d+)] (.*)$");

    @Override
    public String complete(String prompt) {
        Set<String> questionTokens = new HashSet<>();
        String bestSentence = null;
        String bestId = null;
        int bestOverlap = 0;
        for (String line : prompt.split("\\R")) {
            if (line.startsWith("QUESTION: ")) {
                questionTokens.addAll(EmbeddingClient.tokens(line.substring("QUESTION: ".length())));
            }
        }
        for (String line : prompt.split("\\R")) {
            Matcher m = SOURCE.matcher(line);
            if (!m.matches()) continue;
            for (String sentence : m.group(2).split("(?<=\\.)\\s+")) {
                int overlap = (int) EmbeddingClient.tokens(sentence).stream().distinct().filter(questionTokens::contains).count();
                if (overlap > bestOverlap) {
                    bestOverlap = overlap;
                    bestSentence = sentence;
                    bestId = m.group(1);
                }
            }
        }
        return bestSentence == null ? "I don't know." : bestSentence + " [KB-" + bestId + "]";
    }
}
