package com.dke.insurance.underwriting.domain;

public class IllegalTransitionException extends IllegalStateException {

    public IllegalTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super("Cannot move application from " + from + " to " + to);
    }
}
