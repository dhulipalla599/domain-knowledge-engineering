package com.dke.ai_engineering.llm_gateway_and_prompt_management.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Guardrail: masks e-mail addresses and card-like numbers before text leaves the bank or insurer. */
@Component
public class PiiRedactor {

    public record Redacted(String text, int count) {
    }

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    public Redacted redact(String input) {
        int count = 0;
        var email = EMAIL.matcher(input);
        while (email.find()) count++;
        String out = email.replaceAll("[EMAIL]");
        var card = CARD.matcher(out);
        while (card.find()) count++;
        out = card.replaceAll("[CARD]");
        return new Redacted(out, count);
    }
}
