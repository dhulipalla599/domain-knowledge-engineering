package com.dke.data_engineering.real_time_streaming_etl_with_kafka.repository;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.RejectedRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RejectedRecordRepository extends JpaRepository<RejectedRecord, Long> {

    List<RejectedRecord> findByRunId(Long runId);
}
