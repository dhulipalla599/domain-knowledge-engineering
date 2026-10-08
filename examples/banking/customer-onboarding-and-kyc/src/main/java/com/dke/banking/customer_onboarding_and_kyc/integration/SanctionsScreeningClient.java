package com.dke.banking.customer_onboarding_and_kyc.integration;

import com.dke.banking.customer_onboarding_and_kyc.domain.ScreeningOutcome;
import java.time.LocalDate;

/** Port to a sanctions / PEP screening vendor. */
public interface SanctionsScreeningClient {

    ScreeningOutcome screen(String legalName, LocalDate dateOfBirth);
}
