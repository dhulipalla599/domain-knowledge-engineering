package com.dke.ai_engineering.ai_agents_with_tool_calling.integration;

import com.dke.ai_engineering.ai_agents_with_tool_calling.repository.ClaimRepository;
import com.dke.ai_engineering.ai_agents_with_tool_calling.service.Tool;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ScheduleCallbackTool implements Tool {

    private final ClaimRepository claims;

    public ScheduleCallbackTool(ClaimRepository claims) {
        this.claims = claims;
    }

    public String name() { return "schedule_callback"; }

    public String description() { return "Book an adjuster callback for a claim"; }

    public boolean requiresApproval() { return false; }

    public String execute(Map<String, String> arguments) {
        var claim = ClaimArguments.claim(claims, arguments);
        return "Callback booked for " + claim.getClaimNumber() + " on the next business day at 09:00";
    }
}
