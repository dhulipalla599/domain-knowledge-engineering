package com.dke.banking.payments_processing_ach_wire_rtp.integration;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentOrder;

/** Port to the payment network (ACH operator, Fedwire, RTP). */
public interface PaymentNetwork {

    NetworkResponse submit(PaymentOrder order);

    record NetworkResponse(boolean accepted, String reason) {
        public static NetworkResponse ok() { return new NetworkResponse(true, null); }
        public static NetworkResponse refused(String reason) { return new NetworkResponse(false, reason); }
    }
}
