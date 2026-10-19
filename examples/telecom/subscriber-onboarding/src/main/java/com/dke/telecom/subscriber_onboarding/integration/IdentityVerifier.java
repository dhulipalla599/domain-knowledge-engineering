package com.dke.telecom.subscriber_onboarding.integration;

import java.time.LocalDate;

/** Port for the national ID / KYC provider. */
public interface IdentityVerifier {

    Result verify(String fullName, String idDocumentNumber, LocalDate dateOfBirth);

    record Result(boolean verified, String reason) {
    }
}
