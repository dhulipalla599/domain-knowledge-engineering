package com.dke.telecom.number_porting.service;

public class PortRequestNotFound extends RuntimeException {
    public PortRequestNotFound(Long id) {
        super("Port request " + id + " not found");
    }
}
