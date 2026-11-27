package com.dke.retail.store_replenishment.repository;

import com.dke.retail.store_replenishment.domain.OrderStatus;
import com.dke.retail.store_replenishment.domain.ReplenishmentOrder;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplenishmentOrderRepository extends JpaRepository<ReplenishmentOrder, String> {

    Optional<ReplenishmentOrder> findByStoreIdAndSkuAndRunDate(String storeId, String sku, LocalDate runDate);

    boolean existsByStoreIdAndSkuAndStatus(String storeId, String sku, OrderStatus status);

    List<ReplenishmentOrder> findByStoreIdOrderByCreatedAt(String storeId);
}
