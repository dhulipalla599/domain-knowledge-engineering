package com.dke.data_engineering.change_data_capture_cdc_pipelines.repository;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.CustomerReplica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerReplicaRepository extends JpaRepository<CustomerReplica, Long> {

    List<CustomerReplica> findAllByOrderByCustomerIdAsc();
}
