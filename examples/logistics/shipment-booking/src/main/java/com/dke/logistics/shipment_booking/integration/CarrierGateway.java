package com.dke.logistics.shipment_booking.integration;

import com.dke.logistics.shipment_booking.domain.Booking;
import com.dke.logistics.shipment_booking.domain.RateQuote;

/** Port to the carrier's booking system. Production: a REST/EDI adapter with timeouts and retries. */
public interface CarrierGateway {

    /** Returns the carrier's confirmation number, or throws {@link CarrierUnavailableException}. */
    String requestSlot(RateQuote quote, Booking booking);
}
