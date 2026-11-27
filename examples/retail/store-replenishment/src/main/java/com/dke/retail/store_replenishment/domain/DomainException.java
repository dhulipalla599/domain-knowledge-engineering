package com.dke.retail.store_replenishment.domain;

/** Business-rule violations; ApiExceptionHandler maps each one to an HTTP status. */
public abstract sealed class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    public static final class NotFound extends DomainException {
        public NotFound(String what, String id) {
            super(what + " " + id + " not found");
        }
    }

    public static final class CasePackViolation extends DomainException {
        public CasePackViolation(String sku, int quantity, int casePack) {
            super("Quantity " + quantity + " of " + sku + " is not a multiple of the case pack " + casePack);
        }
    }

    public static final class ShelfCapacityExceeded extends DomainException {
        public ShelfCapacityExceeded(String sku, int position, int quantity, int capacity) {
            super("Ordering " + quantity + " of " + sku + " would raise the inventory position from "
                    + position + " to " + (position + quantity) + ", above shelf capacity " + capacity);
        }
    }

    public static final class InvalidQuantity extends DomainException {
        public InvalidQuantity(String message) {
            super(message);
        }
    }

    public static final class InvalidTransition extends DomainException {
        public InvalidTransition(String orderId, OrderStatus from, OrderStatus to) {
            super("Order " + orderId + " cannot move from " + from + " to " + to);
        }
    }
}
