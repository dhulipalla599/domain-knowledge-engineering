package com.dke.ai_engineering.ai_agents_with_tool_calling.integration;

import com.dke.ai_engineering.ai_agents_with_tool_calling.repository.ClaimRepository;
import com.dke.ai_engineering.ai_agents_with_tool_calling.service.Tool;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Moves money, so it always waits for a human approver. */
@Component
public class IssuePaymentTool implements Tool {

    private final ClaimRepository claims;

    public IssuePaymentTool(ClaimRepository claims) {
        this.claims = claims;
    }

    public String name() { return "issue_payment"; }

    public String description() { return "Pay an amount against an approved claim"; }

    public boolean requiresApproval() { return true; }

    public String execute(Map<String, String> arguments) {
        var claim = ClaimArguments.claim(claims, arguments);
        BigDecimal amount;
        try {
            amount = new BigDecimal(arguments.getOrDefault("amount", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("amount must be a number");
        }
        if (amount.signum() <= 0 || amount.compareTo(claim.getApprovedAmount()) > 0) {
            throw new IllegalArgumentException("amount must be between 0 and the approved amount "
                    + claim.getApprovedAmount());
        }
        return "Payment of " + amount + " queued for " + claim.getClaimNumber();
    }
}
