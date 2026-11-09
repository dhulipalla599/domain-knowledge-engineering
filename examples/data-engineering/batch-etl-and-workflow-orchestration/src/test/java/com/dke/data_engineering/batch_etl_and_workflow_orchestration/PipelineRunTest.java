package com.dke.data_engineering.batch_etl_and_workflow_orchestration;

import static org.junit.jupiter.api.Assertions.*;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineRun;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineRun.Trigger;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.RawSale;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.RunStatus;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.service.PipelineService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 8);

    @Test
    void happyPathWalksTheLifecycle() {
        PipelineRun run = new PipelineRun(DAY, Trigger.API);
        run.transitionTo(RunStatus.EXTRACTING);
        run.transitionTo(RunStatus.TRANSFORMING);
        run.transitionTo(RunStatus.LOADING);
        run.transitionTo(RunStatus.SUCCEEDED);
        assertNotNull(run.getFinishedAt());
    }

    @Test
    void cannotSkipStepsOrLeaveATerminalState() {
        PipelineRun run = new PipelineRun(DAY, Trigger.API);
        assertThrows(IllegalStateException.class, () -> run.transitionTo(RunStatus.LOADING));
        run.fail("boom");
        assertThrows(IllegalStateException.class, () -> run.transitionTo(RunStatus.EXTRACTING));
    }

    @Test
    void transformRejectsBadRowsAndDuplicates() {
        var rows = List.of(
                new RawSale("A1", "C1", "S", "2", "10.00", "usd"),
                new RawSale("A1", "C1", "S", "2", "10.00", "usd"),   // duplicate order id
                new RawSale("A2", "C1", "S", "0", "10.00", "usd"),   // zero quantity
                new RawSale("A3", "C1", "S", "x", "10.00", "usd"),   // not a number
                new RawSale("A4", "C1", "S", "1", "10.00", ""));     // missing currency
        var result = PipelineService.transform(rows, DAY);
        assertEquals(1, result.valid().size());
        assertEquals(4, result.rejected());
        assertEquals("USD", result.valid().get(0).getCurrency());
        assertEquals(0, new java.math.BigDecimal("20.00").compareTo(result.valid().get(0).getTotalAmount()));
    }
}
