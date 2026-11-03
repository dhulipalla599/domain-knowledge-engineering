package com.dke.airline.passenger_name_record_pnr.domain;

/** A business rule was broken. {@link Kind} decides the HTTP status in the API layer. */
public class RuleViolationException extends RuntimeException {

    public enum Kind { INVALID, CONFLICT }

    private final Kind kind;

    public RuleViolationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
