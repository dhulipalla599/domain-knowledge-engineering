package com.dke.banking.customer_onboarding_and_kyc.integration;

import com.dke.banking.customer_onboarding_and_kyc.domain.CustomerAccount;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingEvents.ApplicationApproved;
import com.dke.banking.customer_onboarding_and_kyc.repository.CustomerAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Stand-in for the Account Service consuming the application.approved topic.
 * Runs only after the approval is committed, and is idempotent: a redelivered
 * event never opens a second account.
 */
@Component
public class AccountOpeningListener {

    private static final Logger log = LoggerFactory.getLogger(AccountOpeningListener.class);

    private final CustomerAccountRepository accounts;

    public AccountOpeningListener(CustomerAccountRepository accounts) {
        this.accounts = accounts;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ApplicationApproved event) {
        if (accounts.findByApplicationId(event.applicationId()).isPresent()) {
            return;
        }
        String number = "NWB" + String.format("%08d", Math.floorMod(event.applicationId().hashCode(), 100_000_000));
        accounts.save(new CustomerAccount(event.applicationId(), number, event.rating()));
        log.info("account.opened applicationId={} accountNumber={} status=RESTRICTED",
                event.applicationId(), number);
    }
}
