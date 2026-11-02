package com.dke.telecom.number_porting.service;

public class PortRuleViolation extends RuntimeException {
    private final String code;

    public PortRuleViolation(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
