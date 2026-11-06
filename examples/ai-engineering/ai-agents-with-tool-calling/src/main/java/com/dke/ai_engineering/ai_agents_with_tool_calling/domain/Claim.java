package com.dke.ai_engineering.ai_agents_with_tool_calling.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Claim {

    @Id
    private String claimNumber;
    private String customerName;
    private String status;
    private BigDecimal approvedAmount;

    protected Claim() {
    }

    public String getClaimNumber() { return claimNumber; }
    public String getCustomerName() { return customerName; }
    public String getStatus() { return status; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
}
