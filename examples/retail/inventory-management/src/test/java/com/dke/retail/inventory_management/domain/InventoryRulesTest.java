package com.dke.retail.inventory_management.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class InventoryRulesTest {

    @Test
    void reserveReducesAvailableButNotOnHand() {
        var item = new StockItem("SKU-X", "Widget", 10, 2);
        item.reserve(4);
        assertThat(item.available()).isEqualTo(6);
        assertThat(item.getOnHand()).isEqualTo(10);
    }

    @Test
    void cannotReserveMoreThanAvailable() {
        var item = new StockItem("SKU-X", "Widget", 10, 2);
        item.reserve(8);
        assertThatThrownBy(() -> item.reserve(3))
                .isInstanceOf(DomainException.InsufficientStock.class)
                .hasMessageContaining("only 2 available");
    }

    @Test
    void commitRemovesStockPermanently() {
        var item = new StockItem("SKU-X", "Widget", 10, 2);
        item.reserve(4);
        item.commit(4);
        assertThat(item.getOnHand()).isEqualTo(6);
        assertThat(item.available()).isEqualTo(6);
    }

    @Test
    void onlyPendingReservationsCanChangeState() {
        var reservation = new Reservation("ORD-1", "SKU-X", 1, Instant.now());
        reservation.confirm();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThatThrownBy(reservation::release).isInstanceOf(DomainException.InvalidTransition.class);
    }
}
