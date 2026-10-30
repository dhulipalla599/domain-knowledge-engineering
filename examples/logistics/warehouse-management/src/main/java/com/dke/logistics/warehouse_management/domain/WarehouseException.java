package com.dke.logistics.warehouse_management.domain;

/** A business-rule violation; the API layer maps {@link Kind} to an HTTP status. */
public class WarehouseException extends RuntimeException {

    public enum Kind { NOT_FOUND, CONFLICT, UNPROCESSABLE }

    private final Kind kind;

    public WarehouseException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
