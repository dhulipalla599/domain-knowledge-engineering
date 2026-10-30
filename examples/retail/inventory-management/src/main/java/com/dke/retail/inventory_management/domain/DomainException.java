package com.dke.retail.inventory_management.domain;

/** Base type for business-rule violations; the API layer maps each subtype to an HTTP status. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    public static class NotFound extends DomainException {
        public NotFound(String what, String id) {
            super(what + " '" + id + "' not found");
        }
    }

    public static class InsufficientStock extends DomainException {
        public InsufficientStock(String sku, int requested, int available) {
            super("Cannot reserve " + requested + " of " + sku + ": only " + available + " available");
        }
    }

    public static class InvalidTransition extends DomainException {
        public InvalidTransition(ReservationStatus from, ReservationStatus to) {
            super("Reservation cannot move from " + from + " to " + to);
        }
    }
}
