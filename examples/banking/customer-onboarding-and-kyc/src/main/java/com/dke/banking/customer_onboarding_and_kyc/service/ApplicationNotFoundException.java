package com.dke.banking.customer_onboarding_and_kyc.service;

import java.util.UUID;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(UUID id) {
        super("Application " + id + " was not found");
    }
}
