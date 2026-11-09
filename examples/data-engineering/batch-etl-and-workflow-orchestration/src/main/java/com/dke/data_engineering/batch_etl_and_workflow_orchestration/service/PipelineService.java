package com.dke.data_engineering.batch_etl_and_workflow_orchestration.service;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineEvents.RunCompleted;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineEvents.RunFailed;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineEvents.RunStarted;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineRun;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineRun.Trigger;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.RawSale;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.RunStatus;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.SalesFact;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.integration.SourceReader;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.repository.PipelineRunRepository;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.repository.SalesFactRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PipelineService {

    /** Output of the transform step. */
    public record TransformResult(List<SalesFact> valid, int rejected) { }

    private final PipelineRunRepository runs;
    private final SalesFactRepository facts;
    private final SourceReader source;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final double maxRejectRatio;

    public PipelineService(PipelineRunRepository runs, SalesFactRepository facts, SourceReader source,
                           ApplicationEventPublisher events, TransactionTemplate tx,
                           @Value("${etl.max-reject-ratio:0.2}") double maxRejectRatio) {
        this.runs = runs;
        this.facts = facts;
        this.source = source;
        this.events = events;
        this.tx = tx;
        this.maxRejectRatio = maxRejectRatio;
    }

    /**
     * Runs extract -> transform -> load for one business date.
     * Rules: a date that already SUCCEEDED is never reloaded (409); only one active run per date (409);
     * a missing source file is 404; a run whose reject ratio exceeds the threshold FAILS and loads nothing.
     */
    public PipelineRun run(LocalDate businessDate, Trigger trigger) {
        if (runs.existsByBusinessDateAndStatus(businessDate, RunStatus.SUCCEEDED)) {
            throw new PipelineException(HttpStatus.CONFLICT, "Business date " + businessDate + " is already loaded");
        }
        if (runs.existsByBusinessDateAndStatusIn(businessDate, RunStatus.ACTIVE)) {
            throw new PipelineException(HttpStatus.CONFLICT, "A run for " + businessDate + " is already in progress");
        }
        if (!source.exists(businessDate)) {
            throw new PipelineException(HttpStatus.NOT_FOUND, "No source file for " + businessDate);
        }

        PipelineRun run = runs.save(new PipelineRun(businessDate, trigger));
        events.publishEvent(new RunStarted(run.getId(), businessDate));
        try {
            advance(run, RunStatus.EXTRACTING);
            List<RawSale> raw = source.read(businessDate);
            run.recordExtract(raw.size());

            advance(run, RunStatus.TRANSFORMING);
            TransformResult result = transform(raw, businessDate);
            run.recordTransform(result.rejected());
            if (!raw.isEmpty() && (double) result.rejected() / raw.size() > maxRejectRatio) {
                return failRun(run, "Data quality gate: " + result.rejected() + " of " + raw.size()
                        + " rows rejected (max ratio " + maxRejectRatio + ")");
            }

            advance(run, RunStatus.LOADING);
            run.recordLoad(load(result.valid()));
            advance(run, RunStatus.SUCCEEDED);
            events.publishEvent(new RunCompleted(run.getId(), businessDate,
                    run.getRowsExtracted(), run.getRowsLoaded(), run.getRowsRejected()));
            return run;
        } catch (RuntimeException e) {
            return failRun(run, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Validates and types the raw rows; duplicates inside one file keep the first occurrence. */
    public static TransformResult transform(List<RawSale> raw, LocalDate businessDate) {
        List<SalesFact> valid = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int rejected = 0;
        for (RawSale r : raw) {
            try {
                int qty = Integer.parseInt(r.quantity());
                BigDecimal price = new BigDecimal(r.unitPrice());
                boolean ok = !r.orderId().isBlank() && qty > 0 && price.signum() > 0
                        && r.currency().matches("[A-Za-z]{3}") && seen.add(r.orderId());
                if (ok) {
                    valid.add(new SalesFact(r.orderId(), r.customerId(), r.sku(), qty, price,
                            r.currency().toUpperCase(), businessDate));
                } else {
                    rejected++;
                }
            } catch (NumberFormatException e) {
                rejected++;
            }
        }
        return new TransformResult(valid, rejected);
    }

    /** Upserts by order_id inside one transaction, so a failure leaves the warehouse untouched. */
    private int load(List<SalesFact> incoming) {
        return tx.execute(status -> {
            for (SalesFact f : incoming) {
                facts.findByOrderId(f.getOrderId()).ifPresentOrElse(
                        existing -> {
                            existing.overwrite(f.getCustomerId(), f.getSku(), f.getQuantity(),
                                    f.getUnitPrice(), f.getCurrency(), f.getBusinessDate());
                            facts.save(existing);
                        },
                        () -> facts.save(f));
            }
            return incoming.size();
        });
    }

    private void advance(PipelineRun run, RunStatus next) {
        run.transitionTo(next);
        runs.save(run);
    }

    private PipelineRun failRun(PipelineRun run, String reason) {
        run.fail(reason);
        runs.save(run);
        events.publishEvent(new RunFailed(run.getId(), run.getBusinessDate(), reason));
        return run;
    }

    public List<PipelineRun> list() {
        return runs.findAllByOrderByIdDesc();
    }

    public PipelineRun get(Long id) {
        return runs.findById(id).orElseThrow(
                () -> new PipelineException(HttpStatus.NOT_FOUND, "Run " + id + " not found"));
    }
}
