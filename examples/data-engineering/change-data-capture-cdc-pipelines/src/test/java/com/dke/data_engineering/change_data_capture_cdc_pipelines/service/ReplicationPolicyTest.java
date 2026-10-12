package com.dke.data_engineering.change_data_capture_cdc_pipelines.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.ChangeEvent;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.PipelineRun;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.RunStatus;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.service.ReplicationPolicy.Decision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplicationPolicyTest {

    private final ReplicationPolicy policy = new ReplicationPolicy();

    private static ChangeEvent update(long lsn) {
        return new ChangeEvent("u", 1L, lsn, "a@x.example", "Ana", "Porto");
    }

    @Test
    void appliesNewerEventAndFirstSighting() {
        assertThat(policy.decide(update(11), 10L)).isEqualTo(Decision.APPLY);
        assertThat(policy.decide(update(11), null)).isEqualTo(Decision.APPLY);
    }

    @Test
    void skipsStaleAndDuplicateEvents() {
        assertThat(policy.decide(update(9), 10L)).isEqualTo(Decision.SKIP_STALE);
        assertThat(policy.decide(update(10), 10L)).isEqualTo(Decision.SKIP_STALE);
    }

    @Test
    void rejectsMalformedEvents() {
        assertThat(policy.decide(new ChangeEvent("x", 1L, 5, null, null, null), null)).isEqualTo(Decision.REJECT);
        assertThat(policy.decide(new ChangeEvent("u", null, 5, "a@x.example", "Ana", null), null))
                .isEqualTo(Decision.REJECT);
        assertThat(policy.decide(new ChangeEvent("c", 1L, 5, "nope", "Ana", null), null)).isEqualTo(Decision.REJECT);
        assertThat(policy.decide(new ChangeEvent("u", 1L, 0, "a@x.example", "Ana", null), null))
                .isEqualTo(Decision.REJECT);
    }

    @Test
    void deleteNeedsOnlyTheKey() {
        assertThat(policy.decide(new ChangeEvent("d", 1L, 12, null, null, null), 10L)).isEqualTo(Decision.APPLY);
    }

    @Test
    void runLifecycleOnlyAllowsLegalTransitions() {
        PipelineRun run = PipelineRun.pending("s", "API");
        Executable succeedTooEarly = () -> run.succeed(1, 1, 0, 0);
        assertThrows(IllegalStateException.class, succeedTooEarly);
        run.start();
        run.succeed(1, 1, 0, 0);
        assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
        assertThrows(IllegalStateException.class, () -> run.fail("late"));
    }
}
