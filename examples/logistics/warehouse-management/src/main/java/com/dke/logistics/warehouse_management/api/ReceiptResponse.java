package com.dke.logistics.warehouse_management.api;

import com.dke.logistics.warehouse_management.domain.Receipt;
import com.dke.logistics.warehouse_management.domain.ReceiptStatus;

public record ReceiptResponse(Long id, String asnNumber, String sku, int expectedQty, int receivedQty,
                              ReceiptStatus status, String binCode) {

    static ReceiptResponse from(Receipt r) {
        return new ReceiptResponse(r.getId(), r.getAsnNumber(), r.getSku(), r.getExpectedQty(), r.getReceivedQty(),
                r.getStatus(), r.getBinCode());
    }
}
