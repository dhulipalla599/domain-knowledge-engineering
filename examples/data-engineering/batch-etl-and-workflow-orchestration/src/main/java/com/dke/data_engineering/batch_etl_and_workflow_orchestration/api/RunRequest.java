package com.dke.data_engineering.batch_etl_and_workflow_orchestration.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RunRequest(@NotNull LocalDate businessDate) { }
