package com.dke.data_engineering.change_data_capture_cdc_pipelines.domain;

/** One row-level change read from the source database's change log. op is c (insert), u (update) or d (delete). */
public record ChangeEvent(String op, Long customerId, long lsn, String email, String fullName, String city) {
}
