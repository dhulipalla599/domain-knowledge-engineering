package com.dke.ai_engineering.ai_agents_with_tool_calling.integration;

import com.dke.ai_engineering.ai_agents_with_tool_calling.service.LlmClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a hosted model: plans tool calls from keywords in the goal.
 * A real adapter would send the goal, history and tool specs to an LLM tool-calling API.
 */
@Component
public class KeywordPlannerLlmClient implements LlmClient {

    private static final Pattern CLAIM = Pattern.compile("CLM-\\d{4}");
    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*USD");

    @Override
    public Decision decide(String goal, List<Observation> history, List<ToolSpec> tools) {
        for (Observation o : history) {
            if (o.status() != ToolCallStatusView.EXECUTED) {
                return new FinalAnswer("I could not finish: " + o.result());
            }
        }
        Matcher claim = CLAIM.matcher(goal);
        if (!claim.find()) {
            return new FinalAnswer("Please include a claim number such as CLM-1001.");
        }
        String number = claim.group();
        String text = goal.toLowerCase();
        if (text.contains("forever")) {
            return call("lookup_claim", number);
        }
        if (text.contains("delete") && !done(history, "delete_claim")) {
            return call("delete_claim", number);
        }
        if (text.contains("status") && !done(history, "lookup_claim")) {
            return call("lookup_claim", number);
        }
        if (text.contains("callback") && !done(history, "schedule_callback")) {
            return call("schedule_callback", number);
        }
        if (text.contains("pay") && !done(history, "issue_payment")) {
            Matcher amount = AMOUNT.matcher(goal);
            if (!amount.find()) {
                return new FinalAnswer("Please state the payment amount, for example 250 USD.");
            }
            Map<String, String> args = new java.util.HashMap<>(Map.of("claimNumber", number));
            args.put("amount", amount.group(1));
            return new ToolRequest("issue_payment", args);
        }
        List<String> lines = new ArrayList<>();
        history.forEach(o -> lines.add(o.result()));
        return new FinalAnswer(lines.isEmpty() ? "Nothing to do." : lines.stream().collect(Collectors.joining("; ")));
    }

    private static boolean done(List<Observation> history, String tool) {
        return history.stream().anyMatch(o -> o.tool().equals(tool));
    }

    private static ToolRequest call(String tool, String claimNumber) {
        return new ToolRequest(tool, Map.of("claimNumber", claimNumber));
    }
}
