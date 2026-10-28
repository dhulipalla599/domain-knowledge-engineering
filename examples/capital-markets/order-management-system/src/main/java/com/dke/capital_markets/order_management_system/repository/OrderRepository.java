package com.dke.capital_markets.order_management_system.repository;

import com.dke.capital_markets.order_management_system.domain.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByAccountIdAndClientOrderId(String accountId, String clientOrderId);

    List<Order> findByAccountIdOrderByIdDesc(String accountId);
}
