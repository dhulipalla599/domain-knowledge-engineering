package com.dke.insurance.billing_and_premium_collection.domain;

/** Raised when a command would move an installment into a state its lifecycle does not allow. */
public class IllegalTransitionException extends RuntimeException {
    public IllegalTransitionException(String message) {
        super(message);
    }
}
