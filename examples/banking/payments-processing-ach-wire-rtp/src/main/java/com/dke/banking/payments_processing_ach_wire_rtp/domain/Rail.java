package com.dke.banking.payments_processing_ach_wire_rtp.domain;

import java.math.BigDecimal;

/** Payment rails. Limits are illustrative bank policy, not network rules. */
public enum Rail {
    ACH("250000.00", false),     // batch: settles later, can be returned
    WIRE("10000000.00", true),   // real-time gross settlement, irrevocable
    RTP("100000.00", true);      // instant, 24x7, irrevocable

    private final BigDecimal maxAmount;
    private final boolean settlesImmediately;

    Rail(String maxAmount, boolean settlesImmediately) {
        this.maxAmount = new BigDecimal(maxAmount);
        this.settlesImmediately = settlesImmediately;
    }

    public BigDecimal maxAmount() {
        return maxAmount;
    }

    public boolean settlesImmediately() {
        return settlesImmediately;
    }
}
