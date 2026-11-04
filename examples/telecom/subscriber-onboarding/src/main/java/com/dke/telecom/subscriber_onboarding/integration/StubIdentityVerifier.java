package com.dke.telecom.subscriber_onboarding.integration;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** Deterministic stand-in: any document number starting with FAKE fails verification. */
@Component
public class StubIdentityVerifier implements IdentityVerifier {

    @Override
    public Result verify(String fullName, String idDocumentNumber, LocalDate dateOfBirth) {
        if (idDocumentNumber.startsWith("FAKE")) {
            return new Result(false, "Document " + idDocumentNumber + " not found in registry");
        }
        return new Result(true, null);
    }
}
