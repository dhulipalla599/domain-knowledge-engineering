package com.dke.logistics.warehouse_management.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** An inbound receipt created from an advance shipping notice (ASN). */
@Entity
public class Receipt {

    /** Over-receipt tolerance: up to 10% above the ASN quantity is accepted. */
    public static final int OVER_RECEIPT_PERCENT = 10;

    @Id
    @GeneratedValue
    private Long id;
    private String asnNumber;
    private String sku;
    private boolean hazmat;
    private int expectedQty;
    private int receivedQty;
    @Enumerated(EnumType.STRING)
    private ReceiptStatus status = ReceiptStatus.EXPECTED;
    private String binCode;

    protected Receipt() {
    }

    public Receipt(String asnNumber, String sku, boolean hazmat, int expectedQty) {
        this.asnNumber = asnNumber;
        this.sku = sku;
        this.hazmat = hazmat;
        this.expectedQty = expectedQty;
    }

    /** Records the counted quantity at the dock; rejects over-receipt beyond the tolerance. */
    public void receive(int countedQty) {
        requireTransitionTo(ReceiptStatus.RECEIVED);
        if (countedQty <= 0) {
            throw new WarehouseException(WarehouseException.Kind.UNPROCESSABLE, "Counted quantity must be positive");
        }
        int maxAccepted = expectedQty + expectedQty * OVER_RECEIPT_PERCENT / 100;
        if (countedQty > maxAccepted) {
            throw new WarehouseException(WarehouseException.Kind.UNPROCESSABLE,
                    "Over-receipt: counted %d but at most %d accepted for ASN %s".formatted(countedQty, maxAccepted, asnNumber));
        }
        this.receivedQty = countedQty;
        this.status = ReceiptStatus.RECEIVED;
    }

    /** Marks the goods as stored in the given bin. */
    public void putAway(String binCode) {
        requireTransitionTo(ReceiptStatus.PUT_AWAY);
        this.binCode = binCode;
        this.status = ReceiptStatus.PUT_AWAY;
    }

    private void requireTransitionTo(ReceiptStatus next) {
        if (!status.canMoveTo(next)) {
            throw new WarehouseException(WarehouseException.Kind.CONFLICT,
                    "Receipt %s is %s and cannot move to %s".formatted(asnNumber, status, next));
        }
    }

    public Long getId() { return id; }
    public String getAsnNumber() { return asnNumber; }
    public String getSku() { return sku; }
    public boolean isHazmat() { return hazmat; }
    public int getExpectedQty() { return expectedQty; }
    public int getReceivedQty() { return receivedQty; }
    public ReceiptStatus getStatus() { return status; }
    public String getBinCode() { return binCode; }
}
