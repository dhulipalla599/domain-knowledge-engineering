package com.dke.ai_engineering.ai_agents_with_tool_calling.integration;

import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.Claim;
import com.dke.ai_engineering.ai_agents_with_tool_calling.repository.ClaimRepository;
import java.util.Map;
import java.util.regex.Pattern;

/** Validates model-supplied arguments: never trust them. */
final class ClaimArguments {

    private static final Pattern CLAIM_NUMBER = Pattern.compile("CLM-\\d{4}");

    private ClaimArguments() {
    }

    static Claim claim(ClaimRepository claims, Map<String, String> arguments) {
        String number = arguments.getOrDefault("claimNumber", "");
        if (!CLAIM_NUMBER.matcher(number).matches()) {
            throw new IllegalArgumentException("claimNumber must look like CLM-1234");
        }
        return claims.findById(number).orElseThrow(() -> new IllegalArgumentException("Claim " + number + " not found"));
    }
}
