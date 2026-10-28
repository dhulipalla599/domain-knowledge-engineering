package com.dke.insurance.underwriting.domain;

/** Domain events. Each maps to a Kafka topic in production; the topic name is the first accessor. */
public sealed interface UnderwritingEvent {

    String topic();

    Long applicationId();

    /** Topic: underwriting.application-submitted */
    record ApplicationSubmitted(Long applicationId, int riskScore) implements UnderwritingEvent {
        public String topic() { return "underwriting.application-submitted"; }
    }

    /** Topic: underwriting.decision-made */
    record DecisionMade(Long applicationId, ApplicationStatus status, String decidedBy, String reason)
            implements UnderwritingEvent {
        public String topic() { return "underwriting.decision-made"; }
    }
}
