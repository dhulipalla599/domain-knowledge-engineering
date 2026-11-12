package com.dke.retail.order_management_and_fulfillment.repository;

import com.dke.retail.order_management_and_fulfillment.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
