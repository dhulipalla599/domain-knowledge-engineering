package com.dke.airline.flight_shopping_and_booking.service;

import java.util.UUID;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, UUID id) {
        super(what + " " + id + " not found");
    }
}
