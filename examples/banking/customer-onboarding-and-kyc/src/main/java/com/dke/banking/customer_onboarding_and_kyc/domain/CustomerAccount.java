package com.dke.banking.customer_onboarding_and_kyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** The account opened after approval. It starts RESTRICTED until the welcome steps are done. */
@Entity
@Table(name = "customer_account")
public class CustomerAccount {

    public enum AccountStatus { RESTRICTED, ACTIVE }

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false, unique = true)
    private UUID applicationId;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_rating", nullable = false, length = 10)
    private RiskRating riskRating;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    protected CustomerAccount() {
        // for JPA
    }

    public CustomerAccount(UUID applicationId, String accountNumber, RiskRating riskRating) {
        this.id = UUID.randomUUID();
        this.applicationId = applicationId;
        this.accountNumber = accountNumber;
        this.status = AccountStatus.RESTRICTED;
        this.riskRating = riskRating;
        this.openedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public RiskRating getRiskRating() {
        return riskRating;
    }
}
