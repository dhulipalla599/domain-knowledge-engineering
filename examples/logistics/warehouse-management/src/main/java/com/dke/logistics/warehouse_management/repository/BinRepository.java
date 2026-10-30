package com.dke.logistics.warehouse_management.repository;

import com.dke.logistics.warehouse_management.domain.Bin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinRepository extends JpaRepository<Bin, String> {
}
