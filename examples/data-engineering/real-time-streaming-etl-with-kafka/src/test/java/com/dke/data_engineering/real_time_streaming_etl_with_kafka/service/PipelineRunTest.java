package com.dke.data_engineering.real_time_streaming_etl_with_kafka.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.PipelineRun;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.RunStatus;
import org.junit.jupiter.api.Test;

class PipelineRunTest {

    @Test
    void completesWithRejectsWhenAnyRecordWasRejected() {
        PipelineRun run = new PipelineRun("s");
        run.complete(10, 7, 1, 2);
        assertThat(run.getStatus()).isEqualTo(RunStatus.COMPLETED_WITH_REJECTS);
    }

    @Test
    void terminalStateCannotChange() {
        PipelineRun run = new PipelineRun("s");
        run.complete(1, 1, 0, 0);
        assertThatThrownBy(run::fail).isInstanceOf(IllegalStateException.class);
    }
}
