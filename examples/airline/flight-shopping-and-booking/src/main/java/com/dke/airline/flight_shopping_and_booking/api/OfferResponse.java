package com.dke.airline.flight_shopping_and_booking.api;

import com.dke.airline.flight_shopping_and_booking.domain.FlightOffer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record OfferResponse(UUID offerId, String flightNumber, String origin, String destination,
                            LocalDate departureDate, LocalTime departureTime, String fareClass,
                            BigDecimal price, String currency, int seatsAvailable) {

    static OfferResponse from(FlightOffer o) {
        return new OfferResponse(o.getId(), o.getFlightNumber(), o.getOrigin(), o.getDestination(),
                o.getDepartureDate(), o.getDepartureTime(), o.getFareClass(), o.getPrice(), o.getCurrency(),
                o.getSeatsAvailable());
    }
}
