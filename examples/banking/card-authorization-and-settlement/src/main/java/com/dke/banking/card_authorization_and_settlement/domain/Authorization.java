package com.dke.banking.card_authorization_and_settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One authorization request and its hold; cleared later by the network's clearing record. */
@Entity
@Table(name = "authorizations")
public class Authorization {

    @Id
    private UUID id;
    @Column(unique = true, nullable = false, length = 64)
    private String networkReference;
    private String cardToken;
    private String merchantName;
    @Column(length = 4)
    private String mcc;
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    private AuthorizationStatus status;
    @Column(length = 2)
    private String responseCode;
    @Column(length = 6)
    private String approvalCode;
    @Column(precision = 19, scale = 2)
    private BigDecimal clearedAmount;
    private Instant createdAt;
    private Instant updatedAt;

    protected Authorization() {
    }

    private Authorization(String networkReference, String cardToken, String merchantName, String mcc,
                          BigDecimal amount, String currency, AuthorizationStatus status,
                          String responseCode, String approvalCode) {
        this.id = UUID.randomUUID();
        this.networkReference = networkReference;
        this.cardToken = cardToken;
        this.merchantName = merchantName;
        this.mcc = mcc;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.responseCode = responseCode;
        this.approvalCode = approvalCode;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public static Authorization approved(String networkReference, String cardToken, String merchantName,
                                         String mcc, BigDecimal amount, String currency, String approvalCode) {
        return new Authorization(networkReference, cardToken, merchantName, mcc, amount, currency,
                AuthorizationStatus.APPROVED, "00", approvalCode);
    }

    public static Authorization declined(String networkReference, String cardToken, String merchantName,
                                         String mcc, BigDecimal amount, String currency, String responseCode) {
        return new Authorization(networkReference, cardToken, merchantName, mcc, amount, currency,
                AuthorizationStatus.DECLINED, responseCode, null);
    }

    public void reverse() {
        moveTo(AuthorizationStatus.REVERSED);
    }

    public void clear(BigDecimal finalAmount) {
        moveTo(AuthorizationStatus.CLEARED);
        this.clearedAmount = finalAmount;
    }

    private void moveTo(AuthorizationStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Authorization " + id + " is " + status + " and cannot become " + next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    /** True when a replayed request carries the same business content as the stored one. */
    public boolean sameRequest(String cardToken, BigDecimal amount, String currency) {
        return this.cardToken.equals(cardToken) && this.amount.compareTo(amount) == 0
                && this.currency.equals(currency);
    }

    public UUID getId() { return id; }
    public String getNetworkReference() { return networkReference; }
    public String getCardToken() { return cardToken; }
    public String getMerchantName() { return merchantName; }
    public String getMcc() { return mcc; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public AuthorizationStatus getStatus() { return status; }
    public String getResponseCode() { return responseCode; }
    public String getApprovalCode() { return approvalCode; }
    public BigDecimal getClearedAmount() { return clearedAmount; }
    public Instant getCreatedAt() { return createdAt; }
}
