package com.dke.capital_markets.order_management_system.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderTest {

    private Order acceptedBuy() {
        Order o = new Order("ACC-1", "c-1", "ACME", Side.BUY, 100, new BigDecimal("10.00"));
        o.accept(o.notional());
        return o;
    }

    @Test
    void partialThenFullFillReturnsPriceImprovement() {
        Order o = acceptedBuy();
        assertThat(o.applyFill(40, new BigDecimal("9.90"))).isEqualByComparingTo("4.00");
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        o.applyFill(60, new BigDecimal("10.00"));
        assertThat(o.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(o.getReservedCash()).isEqualByComparingTo("0");
    }

    @Test
    void cannotOverfill() {
        assertThatThrownBy(() -> acceptedBuy().applyFill(101, new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void filledOrderCannotBeCanceled() {
        Order o = acceptedBuy();
        o.applyFill(100, new BigDecimal("10.00"));
        assertThatThrownBy(o::cancel).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelReturnsRemainingReservation() {
        Order o = acceptedBuy();
        o.applyFill(25, new BigDecimal("10.00"));
        assertThat(o.cancel()).isEqualByComparingTo("750.00");
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }
}
