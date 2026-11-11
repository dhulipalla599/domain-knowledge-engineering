package com.dke.capital_markets.trade_execution_and_smart_order_routing.repository;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ParentOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentOrderRepository extends JpaRepository<ParentOrder, Long> {

    Optional<ParentOrder> findByClientOrderId(String clientOrderId);
}
