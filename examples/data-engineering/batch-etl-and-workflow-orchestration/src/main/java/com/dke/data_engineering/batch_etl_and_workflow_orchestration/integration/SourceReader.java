package com.dke.data_engineering.batch_etl_and_workflow_orchestration.integration;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.RawSale;
import java.time.LocalDate;
import java.util.List;

/** Extract port. Production adapter: S3 object read (Parquet/CSV) or a JDBC read of the source system. */
public interface SourceReader {

    boolean exists(LocalDate businessDate);

    List<RawSale> read(LocalDate businessDate);
}
