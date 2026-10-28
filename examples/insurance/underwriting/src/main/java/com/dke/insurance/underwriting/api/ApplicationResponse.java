package com.dke.insurance.underwriting.api;

import com.dke.insurance.underwriting.domain.ApplicationStatus;
import com.dke.insurance.underwriting.domain.PolicyApplication;
import java.math.BigDecimal;

public record ApplicationResponse(Long id, String applicantName, BigDecimal sumInsured, int riskScore,
                                  ApplicationStatus status, String decisionReason, String decidedBy) {

    static ApplicationResponse from(PolicyApplication a) {
        return new ApplicationResponse(a.getId(), a.getApplicantName(), a.getSumInsured(), a.getRiskScore(),
                a.getStatus(), a.getDecisionReason(), a.getDecidedBy());
    }
}
