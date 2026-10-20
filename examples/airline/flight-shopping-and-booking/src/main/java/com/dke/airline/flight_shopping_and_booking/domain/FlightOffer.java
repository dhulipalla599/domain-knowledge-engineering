package com.dke.airline.flight_shopping_and_booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** One bookable fare on one flight. Seats are counted per fare, and @Version stops two buyers taking the last one. */
@Entity
@Table(name = "flight_offer")
public class FlightOffer {

    @Id
    private UUID id;

    @Column(name = "flight_number", nullable = false)
    private String flightNumber;

    @Column(nullable = false, length = 3)
    private String origin;

    @Column(nullable = false, length = 3)
    private String destination;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "fare_class", nullable = false)
    private String fareClass;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "seats_available", nullable = false)
    private int seatsAvailable;

    @Version
    private long version;

    protected FlightOffer() {
    }

    public FlightOffer(UUID id, String flightNumber, String origin, String destination, LocalDate departureDate,
                       LocalTime departureTime, String fareClass, BigDecimal price, String currency, int seatsAvailable) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureDate = departureDate;
        this.departureTime = departureTime;
        this.fareClass = fareClass;
        this.price = price;
        this.currency = currency;
        this.seatsAvailable = seatsAvailable;
    }

    /** Takes one seat out of sellable inventory. */
    public void holdSeat() {
        if (seatsAvailable <= 0) {
            throw new IllegalStateException("No seats left on " + flightNumber + " in fare " + fareClass);
        }
        seatsAvailable--;
    }

    public void releaseSeat() {
        seatsAvailable++;
    }

    public UUID getId() { return id; }
    public String getFlightNumber() { return flightNumber; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDate getDepartureDate() { return departureDate; }
    public LocalTime getDepartureTime() { return departureTime; }
    public String getFareClass() { return fareClass; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public int getSeatsAvailable() { return seatsAvailable; }
}
