package com.dke.capital_markets.brokerage_account_opening.service;

import com.dke.capital_markets.brokerage_account_opening.domain.AccountApplication;
import com.dke.capital_markets.brokerage_account_opening.domain.AccountEvents.AccountActivated;
import com.dke.capital_markets.brokerage_account_opening.domain.AccountEvents.ApplicationDecided;
import com.dke.capital_markets.brokerage_account_opening.domain.AccountEvents.ApplicationSubmitted;
import com.dke.capital_markets.brokerage_account_opening.domain.AccountType;
import com.dke.capital_markets.brokerage_account_opening.domain.ApplicationStatus;
import com.dke.capital_markets.brokerage_account_opening.integration.IdentityVerifier;
import com.dke.capital_markets.brokerage_account_opening.integration.SanctionsScreener;
import com.dke.capital_markets.brokerage_account_opening.repository.AccountApplicationRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountOpeningService {

    public record Submission(AccountApplication application, boolean created) {
    }

    private final AccountApplicationRepository repository;
    private final EligibilityPolicy eligibility;
    private final IdentityVerifier identityVerifier;
    private final SanctionsScreener sanctionsScreener;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final AtomicLong accountSequence = new AtomicLong(500_000);

    public AccountOpeningService(AccountApplicationRepository repository, EligibilityPolicy eligibility,
                                 IdentityVerifier identityVerifier, SanctionsScreener sanctionsScreener,
                                 ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.eligibility = eligibility;
        this.identityVerifier = identityVerifier;
        this.sanctionsScreener = sanctionsScreener;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Key business rule: eligibility first, then idempotent persist, then KYC and sanctions screening.
     * A failed identity check rejects; a possible sanctions match goes to a human, never an auto-reject.
     */
    @Transactional
    public Submission submit(String idempotencyKey, String name, LocalDate dateOfBirth, String taxIdLast4,
                             AccountType type, BigDecimal liquidNetWorth) {
        var existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new Submission(existing.get(), false);
        }
        eligibility.check(dateOfBirth, type, liquidNetWorth);

        var application = repository.save(
                new AccountApplication(idempotencyKey, name, dateOfBirth, taxIdLast4, type, liquidNetWorth));
        events.publishEvent(new ApplicationSubmitted(application.getId(), type, Instant.now(clock)));

        if (!identityVerifier.verify(name, dateOfBirth, taxIdLast4)) {
            application.reject("Identity verification failed");
        } else if (sanctionsScreener.possibleMatch(name)) {
            application.sendToReview("Possible sanctions match");
        } else {
            application.approve("Automatic approval: KYC and screening passed");
        }
        publishDecision(application);
        return new Submission(application, true);
    }

    @Transactional
    public AccountApplication review(UUID id, boolean approved, String note) {
        var application = get(id);
        if (application.getStatus() != ApplicationStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only applications in PENDING_REVIEW can be reviewed");
        }
        if (approved) {
            application.approve(note);
        } else {
            application.reject(note);
        }
        publishDecision(application);
        return application;
    }

    @Transactional
    public AccountApplication activate(UUID id) {
        var application = get(id);
        application.activate("BRK-" + accountSequence.incrementAndGet());
        events.publishEvent(new AccountActivated(id, application.getAccountNumber(), Instant.now(clock)));
        return application;
    }

    @Transactional(readOnly = true)
    public AccountApplication get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<AccountApplication> listByStatus(ApplicationStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    private void publishDecision(AccountApplication application) {
        events.publishEvent(new ApplicationDecided(application.getId(), application.getStatus(),
                application.getDecisionReason(), Instant.now(clock)));
    }
}
