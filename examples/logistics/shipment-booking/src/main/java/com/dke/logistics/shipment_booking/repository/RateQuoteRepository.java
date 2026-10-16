package com.dke.logistics.shipment_booking.repository;

import com.dke.logistics.shipment_booking.domain.RateQuote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateQuoteRepository extends JpaRepository<RateQuote, String> {
}
