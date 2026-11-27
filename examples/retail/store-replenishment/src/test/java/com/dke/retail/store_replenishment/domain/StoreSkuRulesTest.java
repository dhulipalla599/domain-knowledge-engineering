package com.dke.retail.store_replenishment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StoreSkuRulesTest {

    // storeId, sku, onHand, onOrder, reorderPoint, orderUpTo, casePack, shelfCapacity
    private StoreSku sku(int onHand, int onOrder) {
        return new StoreSku("S-1", "MILK-2L", onHand, onOrder, 24, 60, 12, 72);
    }

    @Test
    void ordersUpToTargetRoundedUpToWholeCases() {
        assertThat(sku(18, 0).suggestedQuantity()).isEqualTo(48); // needs 42 -> 4 cases
    }

    @Test
    void noOrderAbovePointAndOnOrderStockCounts() {
        assertThat(sku(30, 0).suggestedQuantity()).isZero();
        assertThat(sku(10, 20).suggestedQuantity()).isZero(); // position 30 > 24
        assertThat(sku(24, 0).suggestedQuantity()).isEqualTo(36); // exactly at the point still orders
    }

    @Test
    void roundsDownOneCaseWhenRoundingUpWouldOverflowTheShelf() {
        var eggs = new StoreSku("S-1", "EGGS-12", 5, 0, 12, 36, 15, 40);
        assertThat(eggs.suggestedQuantity()).isEqualTo(30); // 45 would make the position 50 > 40
    }

    @Test
    void manualQuantityMustBeWholeCasesAndFitTheShelf() {
        assertThatThrownBy(() -> sku(18, 0).checkOrderQuantity(20))
                .isInstanceOf(DomainException.CasePackViolation.class);
        assertThatThrownBy(() -> sku(18, 0).checkOrderQuantity(60))
                .isInstanceOf(DomainException.ShelfCapacityExceeded.class);
        assertThatThrownBy(() -> sku(18, 0).checkOrderQuantity(0))
                .isInstanceOf(DomainException.InvalidQuantity.class);
        sku(18, 0).checkOrderQuantity(48); // fits: no exception
    }

    @Test
    void orderLifecycleRejectsIllegalTransitions() {
        var order = new ReplenishmentOrder("S-1", "MILK-2L", 48, "AUTO", null);
        order.release("DC-1");
        assertThatThrownBy(order::cancel).isInstanceOf(DomainException.InvalidTransition.class);
        assertThatThrownBy(() -> order.receive(49)).isInstanceOf(DomainException.InvalidQuantity.class);
        order.receive(44);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
    }
}
