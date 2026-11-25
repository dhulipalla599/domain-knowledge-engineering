package com.dke.data_engineering.data_quality_and_validation.service;

import com.dke.data_engineering.data_quality_and_validation.domain.Claim;
import com.dke.data_engineering.data_quality_and_validation.domain.QualityEvents.BatchFailed;
import com.dke.data_engineering.data_quality_and_validation.domain.QualityEvents.BatchLoaded;
import com.dke.data_engineering.data_quality_and_validation.domain.QualityEvents.BatchQuarantined;
import com.dke.data_engineering.data_quality_and_validation.domain.RuleViolation;
import com.dke.data_engineering.data_quality_and_validation.domain.RunStatus;
import com.dke.data_engineering.data_quality_and_validation.domain.ValidationRun;
import com.dke.data_engineering.data_quality_and_validation.integration.BatchSource;
import com.dke.data_engineering.data_quality_and_validation.integration.BatchSource.SourceFile;
import com.dke.data_engineering.data_quality_and_validation.repository.ClaimRepository;
import com.dke.data_engineering.data_quality_and_validation.repository.RuleViolationRepository;
import com.dke.data_engineering.data_quality_and_validation.repository.ValidationRunRepository;
import com.dke.data_engineering.data_quality_and_validation.service.ClaimValidator.Result;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Extract, validate, gate and load one claims file. */
@Service
public class ValidationService {

    private final BatchSource source;
    private final ClaimValidator validator;
    private final ValidationRunRepository runs;
    private final RuleViolationRepository violations;
    private final ClaimRepository claims;
    private final ApplicationEventPublisher events;
    private final double maxErrorRatio;

    public ValidationService(BatchSource source, ClaimValidator validator, ValidationRunRepository runs,
                             RuleViolationRepository violations, ClaimRepository claims,
                             ApplicationEventPublisher events,
                             @Value("${dq.max-error-ratio}") double maxErrorRatio) {
        this.source = source;
        this.validator = validator;
        this.runs = runs;
        this.violations = violations;
        this.claims = claims;
        this.events = events;
        this.maxErrorRatio = maxErrorRatio;
    }

    @Transactional
    public ValidationRun validate(LocalDate businessDate) {
        SourceFile file = source.read(businessDate)
                .orElseThrow(() -> new QualityException(HttpStatus.NOT_FOUND, "No claims file for " + businessDate));
        runs.findFirstByChecksumAndStatusNot(file.checksum(), RunStatus.FAILED).ifPresent(previous -> {
            throw new QualityException(HttpStatus.CONFLICT,
                    "File " + file.fileName() + " was already processed by run " + previous.getId());
        });

        ValidationRun run = runs.save(new ValidationRun(file.fileName(), file.checksum(), businessDate));
        if (!ClaimValidator.EXPECTED_HEADER.equals(file.header())) {
            String reason = "Schema contract violation: expected " + ClaimValidator.EXPECTED_HEADER + " but got " + file.header();
            run.transitionTo(RunStatus.FAILED, reason);
            events.publishEvent(new BatchFailed(run.getId(), file.fileName(), reason));
            return run;
        }

        run.transitionTo(RunStatus.VALIDATING, null);
        Result result = validator.validate(file.rows(), businessDate);
        run.recordMetrics(file.rows().size(), result.rowsRejected(), result.rowsWarned());
        saveFindings(run.getId(), result);

        if (file.rows().isEmpty() || run.getErrorRatio() > maxErrorRatio) {
            String reason = file.rows().isEmpty()
                    ? "Volume check: file has no data rows"
                    : String.format("Quality gate: %d of %d rows failed (%.1f%% > %.1f%%)", result.rowsRejected(),
                            file.rows().size(), run.getErrorRatio() * 100, maxErrorRatio * 100);
            run.transitionTo(RunStatus.QUARANTINED, reason);
            events.publishEvent(new BatchQuarantined(run.getId(), file.fileName(), businessDate, run.getErrorRatio(), reason));
            return run;
        }

        run.recordLoad(load(run.getId(), result));
        run.transitionTo(RunStatus.LOADED, null);
        events.publishEvent(new BatchLoaded(run.getId(), file.fileName(), businessDate,
                run.getRowsLoaded(), run.getRowsRejected(), false));
        return run;
    }

    /** A data steward accepts a quarantined batch: its valid rows are loaded, bad rows stay quarantined. */
    @Transactional
    public ValidationRun release(Long runId, String approvedBy, String reason) {
        ValidationRun run = get(runId);
        if (run.getStatus() != RunStatus.QUARANTINED) {
            throw new QualityException(HttpStatus.CONFLICT, "Run " + runId + " is " + run.getStatus() + "; only QUARANTINED runs can be released");
        }
        SourceFile file = source.read(run.getBusinessDate())
                .filter(f -> f.checksum().equals(run.getChecksum()))
                .orElseThrow(() -> new QualityException(HttpStatus.CONFLICT, "Source file changed since run " + runId + "; validate it again"));
        Result result = validator.validate(file.rows(), run.getBusinessDate());
        run.release(approvedBy, reason, load(run.getId(), result));
        events.publishEvent(new BatchLoaded(run.getId(), run.getFileName(), run.getBusinessDate(),
                run.getRowsLoaded(), run.getRowsRejected(), true));
        return run;
    }

    @Transactional(readOnly = true)
    public ValidationRun get(Long runId) {
        return runs.findById(runId)
                .orElseThrow(() -> new QualityException(HttpStatus.NOT_FOUND, "Run " + runId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<ValidationRun> list() { return runs.findAllByOrderByIdDesc(); }

    @Transactional(readOnly = true)
    public List<RuleViolation> violations(Long runId) {
        get(runId);
        return violations.findByRunIdOrderByLineNumberAsc(runId);
    }

    private void saveFindings(Long runId, Result result) {
        violations.saveAll(result.findings().stream()
                .map(f -> new RuleViolation(runId, f.lineNumber(), f.ruleCode(), f.severity(), f.message(), f.rawLine()))
                .toList());
    }

    /** Upsert by claim id, so loading the same claim twice never duplicates it. */
    private int load(Long runId, Result result) {
        claims.saveAll(result.valid().stream()
                .map(v -> new Claim(v.claimId(), v.policyNumber(), v.lossDate(), v.amount(), v.currency(), runId))
                .toList());
        return result.valid().size();
    }
}
