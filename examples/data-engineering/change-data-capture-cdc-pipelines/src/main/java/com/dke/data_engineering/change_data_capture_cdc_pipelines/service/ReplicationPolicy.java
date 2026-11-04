package com.dke.data_engineering.change_data_capture_cdc_pipelines.service;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.ChangeEvent;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/** The core CDC rules: reject malformed events, skip stale or duplicate ones, apply the rest. */
@Component
public class ReplicationPolicy {

    public enum Decision { APPLY, SKIP_STALE, REJECT }

    private static final Set<String> OPS = Set.of("c", "u", "d");

    /** currentLsn is the last LSN applied to the target row, or null if the row is not replicated yet. */
    public Decision decide(ChangeEvent event, Long currentLsn) {
        if (validate(event).isPresent()) {
            return Decision.REJECT;
        }
        if (currentLsn != null && event.lsn() <= currentLsn) {
            return Decision.SKIP_STALE;
        }
        return Decision.APPLY;
    }

    /** Returns the reason an event is invalid, or empty if it is fine. */
    public Optional<String> validate(ChangeEvent event) {
        if (event.op() == null || !OPS.contains(event.op())) {
            return Optional.of("unknown operation '" + event.op() + "'");
        }
        if (event.customerId() == null) {
            return Optional.of("missing primary key");
        }
        if (event.lsn() <= 0) {
            return Optional.of("missing or non-positive lsn");
        }
        if (!"d".equals(event.op())) {
            if (event.email() == null || !event.email().contains("@")) {
                return Optional.of("invalid email");
            }
            if (event.fullName() == null || event.fullName().isBlank()) {
                return Optional.of("blank full name");
            }
        }
        return Optional.empty();
    }
}
