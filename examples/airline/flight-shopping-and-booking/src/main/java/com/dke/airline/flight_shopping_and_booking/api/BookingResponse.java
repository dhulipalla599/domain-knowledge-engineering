package com.dke.airline.flight_shopping_and_booking.api;

import com.dke.airline.flight_shopping_and_booking.domain.Booking;
import com.dke.airline.flight_shopping_and_booking.domain.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(UUID id, UUID offerId, String passengerName, BigDecimal totalAmount, String currency,
                              BookingStatus status, Instant holdExpiresAt, String pnr, String ticketNumber,
                              String statusReason) {

    static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getOfferId(), b.getPassengerName(), b.getTotalAmount(),
                b.getCurrency(), b.getStatus(), b.getHoldExpiresAt(), b.getPnr(), b.getTicketNumber(),
                b.getStatusReason());
    }
}
