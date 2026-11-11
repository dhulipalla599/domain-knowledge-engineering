package com.dke.ai_engineering.ai_agents_with_tool_calling.integration;

import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.Claim;
import com.dke.ai_engineering.ai_agents_with_tool_calling.repository.ClaimRepository;
import com.dke.ai_engineering.ai_agents_with_tool_calling.service.Tool;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LookupClaimTool implements Tool {

    private final ClaimRepository claims;

    public LookupClaimTool(ClaimRepository claims) {
        this.claims = claims;
    }

    public String name() { return "lookup_claim"; }

    public String description() { return "Read the status of a claim by claimNumber"; }

    public boolean requiresApproval() { return false; }

    public String execute(Map<String, String> arguments) {
        Claim claim = ClaimArguments.claim(claims, arguments);
        return "Claim " + claim.getClaimNumber() + " for " + claim.getCustomerName() + " is " + claim.getStatus()
                + ", approved amount " + claim.getApprovedAmount();
    }
}
