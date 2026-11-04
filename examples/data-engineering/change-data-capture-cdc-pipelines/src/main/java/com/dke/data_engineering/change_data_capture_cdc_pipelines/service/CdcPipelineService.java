package com.dke.data_engineering.change_data_capture_cdc_pipelines.service;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.ChangeEvent;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.CustomerReplica;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.DomainEvents.CustomerChangeApplied;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.DomainEvents.PipelineRunCompleted;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.PipelineRun;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.RejectedEvent;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.RunStatus;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.repository.CustomerReplicaRepository;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.repository.PipelineRunRepository;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.repository.RejectedEventRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Extract from the change log, validate, transform and load into the replica, recording run metrics. */
@Service
public class CdcPipelineService {

    private final ChangeLogSource source;
    private final ReplicationPolicy policy;
    private final PipelineRunRepository runs;
    private final CustomerReplicaRepository replicas;
    private final RejectedEventRepository rejects;
    private final ApplicationEventPublisher publisher;
    private final TransactionTemplate tx;

    public CdcPipelineService(ChangeLogSource source, ReplicationPolicy policy, PipelineRunRepository runs,
                              CustomerReplicaRepository replicas, RejectedEventRepository rejects,
                              ApplicationEventPublisher publisher, TransactionTemplate tx) {
        this.source = source;
        this.policy = policy;
        this.runs = runs;
        this.replicas = replicas;
        this.rejects = rejects;
        this.publisher = publisher;
        this.tx = tx;
    }

    public PipelineRun run(String sourceName, String triggeredBy) {
        if (runs.existsByStatus(RunStatus.RUNNING)) {
            throw new RunAlreadyInProgressException();
        }
        if (!source.exists(sourceName)) {
            throw new UnknownSourceException(sourceName);
        }
        PipelineRun run = runs.save(PipelineRun.pending(sourceName, triggeredBy));
        run.start();
        runs.save(run);
        try {
            List<ChangeEvent> events = source.read(sourceName).stream()
                    .sorted(Comparator.comparingLong(ChangeEvent::lsn))
                    .toList();
            // One transaction per batch: a failure rolls the whole load back and the run is marked FAILED.
            int[] counts = tx.execute(status -> load(run.getId(), events));
            run.succeed(events.size(), counts[0], counts[1], counts[2]);
        } catch (RuntimeException ex) {
            run.fail(ex.getMessage());
        }
        runs.save(run);
        publisher.publishEvent(new PipelineRunCompleted(
                run.getId(), run.getStatus(), run.getApplied(), run.getSkippedStale(), run.getRejected()));
        return run;
    }

    /** Returns {applied, skippedStale, rejected}. */
    private int[] load(Long runId, List<ChangeEvent> events) {
        int applied = 0;
        int skipped = 0;
        int rejected = 0;
        for (ChangeEvent event : events) {
            CustomerReplica current = event.customerId() == null
                    ? null : replicas.findById(event.customerId()).orElse(null);
            switch (policy.decide(event, current == null ? null : current.getLastLsn())) {
                case REJECT -> {
                    String reason = policy.validate(event).orElse("invalid");
                    rejects.save(new RejectedEvent(runId, event.lsn(), reason, event.toString()));
                    rejected++;
                }
                case SKIP_STALE -> skipped++;
                case APPLY -> {
                    CustomerReplica row = current != null ? current : new CustomerReplica(event.customerId());
                    row.apply(event);
                    replicas.save(row);
                    publisher.publishEvent(new CustomerChangeApplied(event.customerId(), event.op(), event.lsn()));
                    applied++;
                }
            }
        }
        return new int[] {applied, skipped, rejected};
    }
}
