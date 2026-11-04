package com.dke.logistics.warehouse_management.repository;

import com.dke.logistics.warehouse_management.domain.StockLevel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    Optional<StockLevel> findBySkuAndBinCode(String sku, String binCode);
}
