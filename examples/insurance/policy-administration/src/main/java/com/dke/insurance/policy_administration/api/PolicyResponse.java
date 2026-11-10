package com.dke.insurance.policy_administration.api;

import com.dke.insurance.policy_administration.domain.Endorsement;
import com.dke.insurance.policy_administration.domain.Policy;
import com.dke.insurance.policy_administration.domain.PolicyStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PolicyResponse(Long id, String policyNumber, String quoteId, String holderName,
                             BigDecimal sumInsured, BigDecimal annualPremium, LocalDate inception,
                             LocalDate expiry, PolicyStatus status, BigDecimal refundDue,
                             List<EndorsementView> endorsements) {

    public record EndorsementView(LocalDate effectiveDate, BigDecimal newSumInsured,
                                  BigDecimal premiumDelta, String reason) {
        static EndorsementView of(Endorsement e) {
            return new EndorsementView(e.getEffectiveDate(), e.getNewSumInsured(), e.getPremiumDelta(), e.getReason());
        }
    }

    public static PolicyResponse of(Policy p) {
        return new PolicyResponse(p.getId(), p.getPolicyNumber(), p.getQuoteId(), p.getHolderName(),
                p.getSumInsured(), p.getAnnualPremium(), p.getInception(), p.getExpiry(), p.getStatus(),
                p.getRefundDue(), p.getEndorsements().stream().map(EndorsementView::of).toList());
    }
}
