package com.dke.retail.product_catalog_and_pricing.repository;

import com.dke.retail.product_catalog_and_pricing.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
