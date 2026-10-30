package com.dke.logistics.warehouse_management.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReceiptTest {

    @Test
    void acceptsCountWithinOverReceiptTolerance() {
        Receipt receipt = new Receipt("ASN-1", "SKU-1", false, 100);
        receipt.receive(110);
        assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.RECEIVED);
        assertThat(receipt.getReceivedQty()).isEqualTo(110);
    }

    @Test
    void rejectsCountBeyondTolerance() {
        Receipt receipt = new Receipt("ASN-1", "SKU-1", false, 100);
        assertThatThrownBy(() -> receipt.receive(111))
                .isInstanceOf(WarehouseException.class)
                .hasMessageContaining("Over-receipt");
    }

    @Test
    void cannotPutAwayBeforeReceiving() {
        Receipt receipt = new Receipt("ASN-1", "SKU-1", false, 100);
        assertThatThrownBy(() -> receipt.putAway("A-01-01"))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).kind()).isEqualTo(WarehouseException.Kind.CONFLICT);
    }

    @Test
    void binRejectsHazmatInUncertifiedBin() {
        Bin bin = new Bin("A-01-01", 100, 0, false);
        assertThatThrownBy(() -> bin.store(10, true)).isInstanceOf(WarehouseException.class);
    }
}
