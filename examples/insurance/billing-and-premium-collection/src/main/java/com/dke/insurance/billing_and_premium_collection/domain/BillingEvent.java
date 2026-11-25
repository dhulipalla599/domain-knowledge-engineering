package com.dke.insurance.billing_and_premium_collection.domain;

import java.math.BigDecimal;

/** Domain events. Each maps to a Kafka topic in production (see topic()), keyed by policy number. */
public sealed interface BillingEvent {

    String policyNumber();

    String topic();

    record PaymentReceived(String policyNumber, Long installmentId, Long paymentId, BigDecimal amount,
                           InstallmentStatus installmentStatus) implements BillingEvent {
        public String topic() { return "premium.payment.received"; }
    }

    record PaymentFailed(String policyNumber, Long installmentId, BigDecimal amount, String reason)
            implements BillingEvent {
        public String topic() { return "premium.payment.failed"; }
    }

    record InstallmentOverdue(String policyNumber, Long installmentId, BigDecimal outstanding)
            implements BillingEvent {
        public String topic() { return "premium.installment.overdue"; }
    }

    record InstallmentDefaulted(String policyNumber, Long installmentId, BigDecimal outstanding)
            implements BillingEvent {
        public String topic() { return "premium.installment.defaulted"; }
    }
}
