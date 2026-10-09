package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Masks e-mail addresses and card-like digit runs before text is stored or sent to a model. */
@Component
public class PiiRedactor {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    public String redact(String text) {
        return CARD.matcher(EMAIL.matcher(text).replaceAll("[EMAIL]")).replaceAll("[CARD]");
    }
}
