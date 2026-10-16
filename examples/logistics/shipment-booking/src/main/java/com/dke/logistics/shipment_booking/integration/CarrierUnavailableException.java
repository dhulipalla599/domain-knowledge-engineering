package com.dke.logistics.shipment_booking.integration;

public class CarrierUnavailableException extends RuntimeException {

    public CarrierUnavailableException(String message) {
        super(message);
    }
}
