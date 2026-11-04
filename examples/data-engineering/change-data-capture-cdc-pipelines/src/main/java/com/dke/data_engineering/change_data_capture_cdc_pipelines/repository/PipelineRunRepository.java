package com.dke.data_engineering.change_data_capture_cdc_pipelines.repository;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.PipelineRun;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.RunStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, Long> {

    boolean existsByStatus(RunStatus status);

    List<PipelineRun> findAllByOrderByIdDesc();
}
