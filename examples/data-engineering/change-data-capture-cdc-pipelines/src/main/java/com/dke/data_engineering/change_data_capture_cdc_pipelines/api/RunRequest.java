package com.dke.data_engineering.change_data_capture_cdc_pipelines.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** The pattern keeps the name a plain file stem, so a request can never reach outside sample-data/. */
public record RunRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9-]+", message = "must contain only a-z, 0-9 and '-'") String source) {
}
