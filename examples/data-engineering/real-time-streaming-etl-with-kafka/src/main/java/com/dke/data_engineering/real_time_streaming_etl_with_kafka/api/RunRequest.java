package com.dke.data_engineering.real_time_streaming_etl_with_kafka.api;

import jakarta.validation.constraints.NotBlank;

public record RunRequest(@NotBlank String source) {
}
