package com.dke.capital_markets.clearing_and_settlement.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** An executed trade that must be cleared and then settled delivery-versus-payment. */
@Entity
public class Trade {

    @Id
    private String tradeId;
    private String buyerAccount;
    private String sellerAccount;
    private String symbol;
    private long quantity;
    private BigDecimal price;
    private LocalDate tradeDate;
    private LocalDate settlementDate;
    @Enumerated(EnumType.STRING)
    private TradeStatus status = TradeStatus.PENDING_AFFIRMATION;
    private String failReason;
    private Instant settledAt;
    @Version
    private Long version;

    protected Trade() {
    }

    public Trade(String tradeId, String buyerAccount, String sellerAccount, String symbol, long quantity,
                 BigDecimal price, LocalDate tradeDate, LocalDate settlementDate) {
        this.tradeId = tradeId;
        this.buyerAccount = buyerAccount;
        this.sellerAccount = sellerAccount;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.tradeDate = tradeDate;
        this.settlementDate = settlementDate;
    }

    /** Cash the buyer pays the seller on settlement date. */
    public BigDecimal settlementAmount() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public void affirm() {
        moveTo(TradeStatus.AFFIRMED);
    }

    public void settle(Instant at) {
        moveTo(TradeStatus.SETTLED);
        this.settledAt = at;
        this.failReason = null;
    }

    public void fail(String reason) {
        moveTo(TradeStatus.FAILED);
        this.failReason = reason;
    }

    public void cancel() {
        moveTo(TradeStatus.CANCELLED);
    }

    private void moveTo(TradeStatus target) {
        if (!status.next().contains(target)) {
            throw new IllegalStateException("illegal transition " + status + " -> " + target);
        }
        status = target;
    }

    public String getTradeId() { return tradeId; }
    public String getBuyerAccount() { return buyerAccount; }
    public String getSellerAccount() { return sellerAccount; }
    public String getSymbol() { return symbol; }
    public long getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public LocalDate getTradeDate() { return tradeDate; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public TradeStatus getStatus() { return status; }
    public String getFailReason() { return failReason; }
    public Instant getSettledAt() { return settledAt; }
}
