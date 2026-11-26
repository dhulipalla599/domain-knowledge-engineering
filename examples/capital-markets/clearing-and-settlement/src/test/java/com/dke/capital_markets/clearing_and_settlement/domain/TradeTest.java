package com.dke.capital_markets.clearing_and_settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TradeTest {

    private Trade trade() {
        return new Trade("T-1", "A", "B", "ACME", 100, new BigDecimal("10.25"),
                LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 25));
    }

    @Test
    void settlementAmountIsPriceTimesQuantity() {
        assertThat(trade().settlementAmount()).isEqualByComparingTo("1025.00");
    }

    @Test
    void failedTradeCanBeRetriedAndSettled() {
        Trade t = trade();
        t.affirm();
        t.fail("INSUFFICIENT_SECURITIES");
        t.fail("INSUFFICIENT_SECURITIES");
        t.settle(Instant.parse("2026-11-26T20:00:00Z"));
        assertThat(t.getStatus()).isEqualTo(TradeStatus.SETTLED);
        assertThat(t.getFailReason()).isNull();
    }

    @Test
    void unaffirmedTradeCannotSettle() {
        assertThatThrownBy(() -> trade().settle(Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_AFFIRMATION -> SETTLED");
    }

    @Test
    void settledTradeCannotBeCancelled() {
        Trade t = trade();
        t.affirm();
        t.settle(Instant.now());
        assertThatThrownBy(t::cancel).isInstanceOf(IllegalStateException.class);
    }
}
