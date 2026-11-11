package com.dke.capital_markets.trade_execution_and_smart_order_routing.repository;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.Venue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, String> {

    List<Venue> findByEnabledTrue();
}
