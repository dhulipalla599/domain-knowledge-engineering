package com.dke.retail.order_management_and_fulfillment.repository;

import com.dke.retail.order_management_and_fulfillment.domain.FulfillmentNode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FulfillmentNodeRepository extends JpaRepository<FulfillmentNode, String> {
}
