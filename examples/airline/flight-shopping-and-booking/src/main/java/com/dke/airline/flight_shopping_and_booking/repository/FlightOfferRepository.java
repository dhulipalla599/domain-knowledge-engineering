package com.dke.airline.flight_shopping_and_booking.repository;

import com.dke.airline.flight_shopping_and_booking.domain.FlightOffer;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightOfferRepository extends JpaRepository<FlightOffer, UUID> {

    List<FlightOffer> findByOriginAndDestinationAndDepartureDateAndSeatsAvailableGreaterThanOrderByPriceAsc(
            String origin, String destination, LocalDate departureDate, int minSeats);
}
