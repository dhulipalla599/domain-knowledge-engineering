package com.dke.banking.customer_onboarding_and_kyc.domain;

/** Result of sanctions and PEP screening. */
public enum ScreeningOutcome {
    CLEAR,
    POSSIBLE_MATCH,
    CONFIRMED_MATCH
}
