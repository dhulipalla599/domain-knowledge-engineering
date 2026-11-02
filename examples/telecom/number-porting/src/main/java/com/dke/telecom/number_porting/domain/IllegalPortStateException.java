package com.dke.telecom.number_porting.domain;

public class IllegalPortStateException extends RuntimeException {
    public IllegalPortStateException(Long id, PortStatus from, PortStatus to) {
        super("Port request " + id + " cannot move from " + from + " to " + to);
    }
}
