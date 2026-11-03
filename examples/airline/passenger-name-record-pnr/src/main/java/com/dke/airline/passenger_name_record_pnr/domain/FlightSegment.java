package com.dke.airline.passenger_name_record_pnr.domain;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public record FlightSegment(String flightNumber, LocalDate departureDate, String origin, String destination) {
}
