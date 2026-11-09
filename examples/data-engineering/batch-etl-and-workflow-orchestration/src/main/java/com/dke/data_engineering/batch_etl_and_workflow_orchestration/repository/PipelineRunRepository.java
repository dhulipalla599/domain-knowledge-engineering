package com.dke.data_engineering.batch_etl_and_workflow_orchestration.repository;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineRun;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.RunStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, Long> {

    boolean existsByBusinessDateAndStatus(LocalDate businessDate, RunStatus status);

    boolean existsByBusinessDateAndStatusIn(LocalDate businessDate, Collection<RunStatus> statuses);

    List<PipelineRun> findAllByOrderByIdDesc();
}
