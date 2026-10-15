package com.dke.retail.product_catalog_and_pricing.service;

/** Business-rule failures; the API layer maps each subtype to an HTTP status. */
public abstract class PricingException extends RuntimeException {

    protected PricingException(String message) {
        super(message);
    }

    public static class InvalidPrice extends PricingException {
        public InvalidPrice(String message) { super(message); }
    }

    public static class BelowMarginFloor extends PricingException {
        public BelowMarginFloor(String message) { super(message); }
    }

    public static class NotFound extends PricingException {
        public NotFound(String message) { super(message); }
    }

    public static class Conflict extends PricingException {
        public Conflict(String message) { super(message); }
    }
}
