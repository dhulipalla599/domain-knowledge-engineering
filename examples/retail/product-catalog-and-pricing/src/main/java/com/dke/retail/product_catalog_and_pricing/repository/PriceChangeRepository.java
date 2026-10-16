package com.dke.retail.product_catalog_and_pricing.repository;

import com.dke.retail.product_catalog_and_pricing.domain.PriceChange;
import com.dke.retail.product_catalog_and_pricing.domain.PriceChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceChangeRepository extends JpaRepository<PriceChange, Long> {

    boolean existsBySkuAndStatus(String sku, PriceChangeStatus status);
}
