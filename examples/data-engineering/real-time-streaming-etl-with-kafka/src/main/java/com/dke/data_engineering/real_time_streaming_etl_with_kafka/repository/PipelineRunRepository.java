package com.dke.data_engineering.real_time_streaming_etl_with_kafka.repository;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.PipelineRun;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.RunStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, Long> {

    boolean existsBySourceAndStatus(String source, RunStatus status);

    List<PipelineRun> findAllByOrderByIdDesc();
}
