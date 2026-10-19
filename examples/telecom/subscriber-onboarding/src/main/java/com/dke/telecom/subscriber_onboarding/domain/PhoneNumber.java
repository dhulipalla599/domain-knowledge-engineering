package com.dke.telecom.subscriber_onboarding.domain;

import jakarta.persistence.*;

@Entity
public class PhoneNumber {

    public enum Status { AVAILABLE, IN_USE }

    @Id
    private String msisdn;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Version
    private Long version;

    protected PhoneNumber() {
    }

    public void markInUse() {
        if (status != Status.AVAILABLE) {
            throw new IllegalStateException("Number " + msisdn + " is not available");
        }
        this.status = Status.IN_USE;
    }

    public String getMsisdn() { return msisdn; }
    public Status getStatus() { return status; }
}
