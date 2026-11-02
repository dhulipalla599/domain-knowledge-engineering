package com.dke.telecom.number_porting.integration;

/** Asks the donor operator whether it accepts the port. Real adapter: central porting hub / operator API. */
public interface DonorGateway {

    record Decision(boolean accepted, String reason) {
        public static Decision accept() { return new Decision(true, null); }
        public static Decision reject(String reason) { return new Decision(false, reason); }
    }

    Decision validate(String msisdn, String subscriberName, String donorOperator);
}
