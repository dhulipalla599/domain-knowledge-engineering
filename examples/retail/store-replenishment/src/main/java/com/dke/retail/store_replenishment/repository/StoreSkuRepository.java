package com.dke.retail.store_replenishment.repository;

import com.dke.retail.store_replenishment.domain.StoreSku;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSkuRepository extends JpaRepository<StoreSku, Long> {

    Optional<StoreSku> findByStoreIdAndSku(String storeId, String sku);

    List<StoreSku> findByStoreIdOrderBySku(String storeId);
}
