package com.dke.airline.passenger_name_record_pnr.domain;

import jakarta.persistence.Embeddable;

/** Name exactly as it must appear on the travel document, stored upper-case. */
@Embeddable
public record PassengerName(String firstName, String lastName) {
}
