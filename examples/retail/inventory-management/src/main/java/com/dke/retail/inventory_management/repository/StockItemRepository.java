package com.dke.retail.inventory_management.repository;

import com.dke.retail.inventory_management.domain.StockItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    Optional<StockItem> findBySku(String sku);
}
