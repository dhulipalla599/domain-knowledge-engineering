package com.dke.capital_markets.clearing_and_settlement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Domain events. Each record stands in for the Kafka topic named in its Javadoc. */
public final class ClearingEvents {

    private ClearingEvents() {
    }

    /** Topic clearing.trade-captured.v1 */
    public record TradeCaptured(String tradeId, String buyerAccount, String sellerAccount, String symbol,
                                long quantity, BigDecimal price, LocalDate settlementDate) {
    }

    /** Topic clearing.trade-affirmed.v1 */
    public record TradeAffirmed(String tradeId, LocalDate settlementDate) {
    }

    /** Topic settlement.trade-settled.v1 */
    public record TradeSettled(String tradeId, String symbol, long quantity, BigDecimal amount, LocalDate settlementDate) {
    }

    /** Topic settlement.trade-failed.v1 */
    public record SettlementFailed(String tradeId, String reason, LocalDate settlementDate) {
    }
}
