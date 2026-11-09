package com.dke.data_engineering.batch_etl_and_workflow_orchestration.repository;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.SalesFact;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesFactRepository extends JpaRepository<SalesFact, Long> {

    Optional<SalesFact> findByOrderId(String orderId);
}
