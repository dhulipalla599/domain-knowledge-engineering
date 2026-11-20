package com.dke.healthcare.insurance_eligibility_verification.integration;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Port to the payer, normally through a clearinghouse (X12 270 request, 271 response).
 * The example uses {@link StubPayerGateway}; production uses a real clearinghouse adapter.
 */
public interface PayerGateway {

    PayerResponse inquire(EligibilityInquiry inquiry);

    /** What goes into the 270: who the member is, which date and which kind of service. */
    record EligibilityInquiry(String payerId, String memberId, String subscriberFirstName, String subscriberLastName,
                              LocalDate subscriberDateOfBirth, LocalDate serviceDate, String serviceTypeCode) {
    }

    /** What comes back in the 271. A non-null rejectReasonCode means the payer could not answer (AAA segment). */
    record PayerResponse(boolean activeCoverage, LocalDate planBeginDate, LocalDate planEndDate, BigDecimal copayAmount,
                         BigDecimal deductibleRemaining, String rejectReasonCode, String message) {

        static PayerResponse rejected(String code, String message) {
            return new PayerResponse(false, null, null, null, null, code, message);
        }
    }

    /** Timeout or outage; the inquiry can be retried later. */
    class PayerUnavailableException extends RuntimeException {
        public PayerUnavailableException(String message) {
            super(message);
        }
    }
}
