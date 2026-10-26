package com.dke.data_engineering.real_time_streaming_etl_with_kafka.service;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.DomainEvents.*;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.OrderFact;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.PipelineRun;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.RejectedRecord;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.RunStatus;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.repository.OrderFactRepository;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.repository.PipelineRunRepository;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.repository.RejectedRecordRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class StreamingEtlService {

    private final RecordSource source;
    private final OrderRecordTransformer transformer;
    private final PipelineRunRepository runs;
    private final OrderFactRepository facts;
    private final RejectedRecordRepository rejects;
    private final ApplicationEventPublisher events;

    public StreamingEtlService(RecordSource source, OrderRecordTransformer transformer,
                               PipelineRunRepository runs, OrderFactRepository facts,
                               RejectedRecordRepository rejects, ApplicationEventPublisher events) {
        this.source = source;
        this.transformer = transformer;
        this.runs = runs;
        this.facts = facts;
        this.rejects = rejects;
        this.events = events;
    }

    /**
     * Business rules: one RUNNING run per source; a bad line goes to the dead-letter table
     * instead of failing the batch; an eventId that is already loaded is skipped (idempotent replay).
     */
    public PipelineRun run(String sourceName) {
        if (runs.existsBySourceAndStatus(sourceName, RunStatus.RUNNING)) {
            throw new RunAlreadyInProgressException(sourceName);
        }
        List<String> lines = source.readLines(sourceName);
        PipelineRun run = runs.save(new PipelineRun(sourceName));
        events.publishEvent(new OrdersRaw(run.getId(), sourceName, lines.size()));

        int loaded = 0, duplicates = 0, rejected = 0;
        try {
            for (String line : lines) {
                try {
                    OrderFact fact = transformer.transform(line);
                    if (facts.existsByEventId(fact.getEventId())) {
                        duplicates++;
                        continue;
                    }
                    facts.save(fact);
                    loaded++;
                    events.publishEvent(new OrdersCurated(run.getId(), fact.getEventId(), fact.getOrderId(),
                            fact.getCountry(), fact.getCurrency(), fact.getAmountCents()));
                } catch (InvalidRecordException e) {
                    rejected++;
                    rejects.save(new RejectedRecord(run.getId(), line, e.getMessage()));
                    events.publishEvent(new OrdersDlq(run.getId(), line, e.getMessage()));
                }
            }
            run.complete(lines.size(), loaded, duplicates, rejected);
        } catch (RuntimeException e) {
            run.fail();
            runs.save(run);
            throw e;
        }
        runs.save(run);
        events.publishEvent(new PipelineRunCompleted(run.getId(), run.getStatus(), loaded, duplicates, rejected));
        return run;
    }
}
