package com.dke.logistics.shipment_booking.integration;

import com.dke.logistics.shipment_booking.domain.Booking;
import com.dke.logistics.shipment_booking.domain.RateQuote;
import org.springframework.stereotype.Component;

/** Deterministic stand-in: confirms everything except carrier "OFFLINE-LINES". */
@Component
public class StubCarrierGateway implements CarrierGateway {

    @Override
    public String requestSlot(RateQuote quote, Booking booking) {
        if ("OFFLINE-LINES".equals(quote.getCarrier())) {
            throw new CarrierUnavailableException("Carrier " + quote.getCarrier() + " is not responding");
        }
        return quote.getCarrier().substring(0, 3).toUpperCase() + "-" + booking.getReference();
    }
}
