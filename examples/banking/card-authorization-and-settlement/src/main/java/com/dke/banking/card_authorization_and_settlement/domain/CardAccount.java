package com.dke.banking.card_authorization_and_settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

/** A credit card account as the issuer sees it: limit, posted balance and open holds. */
@Entity
@Table(name = "card_accounts")
public class CardAccount {

    @Id
    @Column(name = "card_token", length = 32)
    private String cardToken;
    private String holderName;
    @Column(length = 3)
    private String currency;
    @Column(precision = 19, scale = 2)
    private BigDecimal creditLimit;
    @Column(precision = 19, scale = 2)
    private BigDecimal postedBalance;
    @Column(precision = 19, scale = 2)
    private BigDecimal pendingHolds;
    @Enumerated(EnumType.STRING)
    private CardStatus status;
    @Version
    private long version;

    protected CardAccount() {
    }

    public CardAccount(String cardToken, String holderName, String currency, BigDecimal creditLimit) {
        this.cardToken = cardToken;
        this.holderName = holderName;
        this.currency = currency;
        this.creditLimit = creditLimit;
        this.postedBalance = BigDecimal.ZERO;
        this.pendingHolds = BigDecimal.ZERO;
        this.status = CardStatus.ACTIVE;
    }

    /** Open-to-buy: what the cardholder can still spend right now. */
    public BigDecimal availableCredit() {
        return creditLimit.subtract(postedBalance).subtract(pendingHolds);
    }

    public void placeHold(BigDecimal amount) {
        pendingHolds = pendingHolds.add(amount);
    }

    public void releaseHold(BigDecimal amount) {
        pendingHolds = pendingHolds.subtract(amount);
    }

    /** Clearing turns the hold into a posted transaction for the final amount. */
    public void postCleared(BigDecimal heldAmount, BigDecimal clearedAmount) {
        releaseHold(heldAmount);
        postedBalance = postedBalance.add(clearedAmount);
    }

    public String getCardToken() { return cardToken; }
    public String getHolderName() { return holderName; }
    public String getCurrency() { return currency; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public BigDecimal getPostedBalance() { return postedBalance; }
    public BigDecimal getPendingHolds() { return pendingHolds; }
    public CardStatus getStatus() { return status; }
}
