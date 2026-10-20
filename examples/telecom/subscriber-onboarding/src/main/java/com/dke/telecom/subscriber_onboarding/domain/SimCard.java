package com.dke.telecom.subscriber_onboarding.domain;

import jakarta.persistence.*;

@Entity
public class SimCard {

    public enum Status { AVAILABLE, ASSIGNED }

    @Id
    private String iccid;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String msisdn;
    @Version
    private Long version;

    protected SimCard() {
    }

    public boolean isAvailable() {
        return status == Status.AVAILABLE;
    }

    public void assign(String msisdn) {
        this.status = Status.ASSIGNED;
        this.msisdn = msisdn;
    }

    public String getIccid() { return iccid; }
    public Status getStatus() { return status; }
    public String getMsisdn() { return msisdn; }
}
