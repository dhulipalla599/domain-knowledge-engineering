package com.dke.logistics.warehouse_management.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A storage location with a unit capacity; some bins are certified for hazardous goods. */
@Entity
public class Bin {

    @Id
    private String code;
    private int capacityUnits;
    private int usedUnits;
    private boolean hazmatCertified;

    protected Bin() {
    }

    public Bin(String code, int capacityUnits, int usedUnits, boolean hazmatCertified) {
        this.code = code;
        this.capacityUnits = capacityUnits;
        this.usedUnits = usedUnits;
        this.hazmatCertified = hazmatCertified;
    }

    /** Reserves space for the goods, enforcing hazmat certification and capacity. */
    public void store(int qty, boolean hazmat) {
        if (hazmat && !hazmatCertified) {
            throw new WarehouseException(WarehouseException.Kind.UNPROCESSABLE,
                    "Bin %s is not certified for hazardous goods".formatted(code));
        }
        if (usedUnits + qty > capacityUnits) {
            throw new WarehouseException(WarehouseException.Kind.CONFLICT,
                    "Bin %s has %d free units but %d are needed".formatted(code, capacityUnits - usedUnits, qty));
        }
        usedUnits += qty;
    }

    public String getCode() { return code; }
    public int getCapacityUnits() { return capacityUnits; }
    public int getUsedUnits() { return usedUnits; }
    public boolean isHazmatCertified() { return hazmatCertified; }
}
