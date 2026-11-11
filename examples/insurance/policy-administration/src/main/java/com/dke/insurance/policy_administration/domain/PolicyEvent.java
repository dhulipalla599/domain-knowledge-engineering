package com.dke.insurance.policy_administration.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Domain events. Each maps to a Kafka topic in production (see topic()). */
public sealed interface PolicyEvent {

    Long policyId();

    String topic();

    record PolicyIssued(Long policyId, String policyNumber, BigDecimal annualPremium) implements PolicyEvent {
        public String topic() { return "policy.issued"; }
    }

    record PolicyEndorsed(Long policyId, LocalDate effectiveDate, BigDecimal premiumDelta) implements PolicyEvent {
        public String topic() { return "policy.endorsed"; }
    }

    record PolicyCancelled(Long policyId, LocalDate effectiveDate, BigDecimal refundDue) implements PolicyEvent {
        public String topic() { return "policy.cancelled"; }
    }

    record PolicyExpired(Long policyId) implements PolicyEvent {
        public String topic() { return "policy.expired"; }
    }
}
