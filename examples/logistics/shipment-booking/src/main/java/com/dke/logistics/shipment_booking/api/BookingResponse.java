package com.dke.logistics.shipment_booking.api;

import com.dke.logistics.shipment_booking.domain.Booking;
import com.dke.logistics.shipment_booking.domain.BookingStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record BookingResponse(UUID id, String reference, BookingStatus status, String quoteId, double weightKg,
                              BigDecimal price, String currency, String carrierConfirmation) {

    static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getReference(), b.getStatus(), b.getQuoteId(), b.getWeightKg(),
                b.getPrice(), b.getCurrency(), b.getCarrierConfirmation());
    }
}
