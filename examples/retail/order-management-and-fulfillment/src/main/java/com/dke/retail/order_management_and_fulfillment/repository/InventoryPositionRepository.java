package com.dke.retail.order_management_and_fulfillment.repository;

import com.dke.retail.order_management_and_fulfillment.domain.InventoryPosition;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryPositionRepository extends JpaRepository<InventoryPosition, Long> {

    List<InventoryPosition> findBySkuIn(Collection<String> skus);

    List<InventoryPosition> findBySkuOrderByNodeCode(String sku);
}
