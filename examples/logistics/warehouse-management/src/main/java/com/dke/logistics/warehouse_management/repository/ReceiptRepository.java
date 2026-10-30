package com.dke.logistics.warehouse_management.repository;

import com.dke.logistics.warehouse_management.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
}
