package com.dke.data_engineering.change_data_capture_cdc_pipelines.repository;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.RejectedEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RejectedEventRepository extends JpaRepository<RejectedEvent, Long> {

    List<RejectedEvent> findByRunIdOrderByLsnAsc(Long runId);
}
