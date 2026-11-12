package com.dke.retail.order_management_and_fulfillment.repository;

import com.dke.retail.order_management_and_fulfillment.domain.CustomerOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey);

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
}
